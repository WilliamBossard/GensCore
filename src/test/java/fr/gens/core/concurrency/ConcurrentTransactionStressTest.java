package fr.gens.core.concurrency;

import fr.gens.core.modules.teams.TeamData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de tests de stress et de concurrence sous forte contention multithread.
 * Valide l'atomicité, la consistance et l'absence de race conditions ou de deadlocks
 * pour l'économie, les banques de guilde, et les transactions SQLite.
 */
public class ConcurrentTransactionStressTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test: Concurrence extrême sur la banque de guilde (50 threads simultanés)")
    void testConcurrentTeamBankDepositsAndWithdrawals() throws InterruptedException {
        TeamData team = new TeamData(42, "StressGuild", UUID.randomUUID());
        team.setBankBalance(10_000.0);

        int numThreads = 50;
        int operationsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        AtomicInteger successfulWithdrawals = new AtomicInteger(0);
        AtomicInteger failedWithdrawals = new AtomicInteger(0);

        // 25 threads qui déposent 10.0, 25 threads qui tentent de retirer 20.0
        for (int i = 0; i < numThreads; i++) {
            final boolean isDeposit = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startGate.await(); // Synchronisation pour un départ simultané
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (isDeposit) {
                            team.addBankBalance(10.0);
                        } else {
                            boolean ok = team.withdrawBankBalance(20.0);
                            if (ok) {
                                successfulWithdrawals.incrementAndGet();
                            } else {
                                failedWithdrawals.incrementAndGet();
                            }
                        }
                    }
                } catch (Exception e) {
                    fail("Exception dans le worker thread: " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Lancement immédiat de tous les threads
        startGate.countDown();
        assertTrue(endGate.await(8, TimeUnit.SECONDS), "Les threads de test n'ont pas terminé à temps");
        executor.shutdown();

        // Vérifications formelles
        double finalBalance = team.getBankBalance();
        assertTrue(finalBalance >= 0.0, "Le solde de la banque ne doit JAMAIS être négatif");

        int totalDeposits = (numThreads / 2) * operationsPerThread;
        double expectedBalance = 10_000.0 + (totalDeposits * 10.0) - (successfulWithdrawals.get() * 20.0);

        assertEquals(expectedBalance, finalBalance, 0.001,
                "Incohérence détectée entre les opérations atomiques et le solde bancaire final");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test: Compétition atomique de retrait (Contention sur solde limité sans découvert)")
    void testAtomicBalanceContentionNoOverdraft() throws InterruptedException {
        // Simulation exacte du pattern ConcurrentHashMap.compute() utilisé dans EconomyModule.takeMoneyAtomic()
        Map<UUID, Double> balances = new ConcurrentHashMap<>();
        UUID playerId = UUID.randomUUID();
        double initialBalance = 500.0;
        balances.put(playerId, initialBalance);

        int numThreads = 40;
        int attemptsPerThread = 50;
        double amountPerAttempt = 5.0; // 40 * 50 * 5.0 = 10 000.0 demandé, mais seulement 500.0 dispo (100 retraits max)

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < attemptsPerThread; j++) {
                        AtomicBoolean success = new AtomicBoolean(false);
                        balances.compute(playerId, (k, current) -> {
                            double bal = (current == null ? 0.0 : current);
                            if (bal >= amountPerAttempt) {
                                success.set(true);
                                return bal - amountPerAttempt;
                            }
                            return bal;
                        });

                        if (success.get()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    fail("Exception in worker: " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(8, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactement 100 retraits de 5.0 doivent avoir réussi (500.0 / 5.0 = 100)
        assertEquals(100, successCount.get(), "Exactement 100 retraits devaient réussir sur 500$ de solde");
        assertEquals((numThreads * attemptsPerThread) - 100, failureCount.get());
        assertEquals(0.0, balances.get(playerId), 0.0001, "Le solde final doit être strictement 0.0$");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test: Achat concurrent unique sur l'Hôtel des Ventes (Simulation Anti-Duplication)")
    void testAuctionHouseAtomicBuyContention() throws Exception {
        // En base de données SQLite : un seul acheteur doit pouvoir réclamer l'enchère
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE test_auctions (id INTEGER PRIMARY KEY, item VARCHAR(64), sold INTEGER DEFAULT 0, buyer VARCHAR(36));");
                stmt.execute("INSERT INTO test_auctions (id, item, sold) VALUES (1, 'DIAMOND_SWORD', 0);");
            }

            int buyerThreads = 30;
            ExecutorService executor = Executors.newFixedThreadPool(buyerThreads);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch endGate = new CountDownLatch(buyerThreads);

            AtomicInteger purchaseSuccess = new AtomicInteger(0);
            AtomicInteger purchaseDenied = new AtomicInteger(0);

            for (int i = 0; i < buyerThreads; i++) {
                final UUID buyerUuid = UUID.randomUUID();
                executor.submit(() -> {
                    try {
                        startGate.await();
                        // Transaction d'achat atomique : UPDATE ... WHERE id = 1 AND sold = 0
                        synchronized (conn) {
                            try (PreparedStatement ps = conn.prepareStatement("UPDATE test_auctions SET sold = 1, buyer = ? WHERE id = 1 AND sold = 0")) {
                                ps.setString(1, buyerUuid.toString());
                                int updated = ps.executeUpdate();
                                if (updated > 0) {
                                    purchaseSuccess.incrementAndGet();
                                } else {
                                    purchaseDenied.incrementAndGet();
                                }
                            }
                        }
                    } catch (Exception e) {
                        fail("Exception during auction buy: " + e.getMessage());
                    } finally {
                        endGate.countDown();
                    }
                });
            }

            startGate.countDown();
            assertTrue(endGate.await(8, TimeUnit.SECONDS));
            executor.shutdown();

            // Exactement UN acheteur gagne, tous les autres sont rejetés
            assertEquals(1, purchaseSuccess.get(), "Exactement UN acheteur doit acquérir l'objet mis aux enchères");
            assertEquals(buyerThreads - 1, purchaseDenied.get(), "Tous les autres acheteurs simultanés doivent être rejetés");

            // Vérification de l'état final en BDD
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT sold, buyer FROM test_auctions WHERE id = 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("sold"));
                assertNotNull(rs.getString("buyer"));
            }
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test: Gestion concurrente des membres et rôles d'une guilde")
    void testConcurrentTeamMemberModifications() throws InterruptedException {
        TeamData team = new TeamData(10, "RoleStressGuild", UUID.randomUUID());

        int numThreads = 30;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final UUID member = UUID.randomUUID();
            executor.submit(() -> {
                try {
                    startGate.await();
                    team.addMember(member);
                    team.promoteAdmin(member);
                    assertEquals("ADMIN", team.getRoleName(member));
                    team.demoteAdmin(member);
                    assertEquals("MEMBER", team.getRoleName(member));
                } catch (Exception e) {
                    fail("Concurrence exception in member ops: " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(8, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(numThreads + 1, team.getMembers().size(), "30 nouveaux membres + 1 leader initial");
        assertEquals(0, team.getAdmins().size());
    }
}
