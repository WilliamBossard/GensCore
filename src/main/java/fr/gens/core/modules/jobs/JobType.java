package fr.gens.core.modules.jobs;

public enum JobType {
    MINEUR("Mineur", "§7", "Pioche"),
    BUCHERON("Bûcheron", "§a", "Hache"),
    FERMIER("Fermier", "§e", "Houe"),
    CHASSEUR("Chasseur", "§c", "Épée");

    private final String displayName;
    private final String color;
    private final String iconName;

    JobType(String displayName, String color, String iconName) {
        this.displayName = displayName;
        this.color = color;
        this.iconName = iconName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public String getIconName() {
        return iconName;
    }
}
