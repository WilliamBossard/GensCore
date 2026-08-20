package fr.gens.core.utils;

import org.bukkit.Bukkit;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;

import java.util.List;


public class GensScoreboard {


    private final Scoreboard scoreboard;
    private final Objective objective;

    public GensScoreboard(Player player, String title) {

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("gensboard", org.bukkit.scoreboard.Criteria.DUMMY, fr.gens.core.utils.PlaceholderUtils.parseToComponent(title));
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
        // Le scoreboard supporte jusqu'ÃƒÆ’Ã‚Â  15 lignes
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
            
            // Mettre ÃƒÆ’Ã‚Â  jour ou crÃƒÆ’Ã‚Â©er
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
        return "§" + "0123456789abcdef".charAt(line) + "§r";
    }
}

