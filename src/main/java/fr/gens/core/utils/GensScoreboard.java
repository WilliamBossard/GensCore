package fr.gens.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

public class GensScoreboard {

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Player player;

    public GensScoreboard(Player player, String title) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("gensboard", "dummy", title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        try {
            // Tentative de masquage des chiffres (Paper 1.20.3+)
            this.objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Throwable ignored) {}
        
        player.setScoreboard(this.scoreboard);
    }

    public void updateTitle(String title) {
        Component titleComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(title);
        objective.displayName(titleComp);
    }

    public void updateLines(List<String> lines) {
        // Le scoreboard supporte jusqu'à 15 lignes
        int size = Math.min(15, lines.size());
        
        for (int i = 0; i < 15; i++) {
            String teamName = "line_" + i;
            Team team = scoreboard.getTeam(teamName);
            
            // Si la ligne ne doit plus exister
            if (i >= size) {
                if (team != null) {
                    scoreboard.resetScores(getEntry(i));
                    team.unregister();
                }
                continue;
            }
            
            // Mettre à jour ou créer
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.addEntry(getEntry(i));
                objective.getScore(getEntry(i)).setScore(15 - i);
            }
            
            String text = lines.get(i);
            Component finalComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(text);
            team.prefix(finalComp);
            team.suffix(Component.empty());
        }
    }
    
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    private String getEntry(int line) {
        return ChatColor.values()[line].toString() + ChatColor.RESET;
    }
}
