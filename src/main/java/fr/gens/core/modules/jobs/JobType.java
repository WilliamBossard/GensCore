package fr.gens.core.modules.jobs;

public enum JobType {
    MINEUR("Mineur", "<gray>", "Pioche"),
    BUCHERON("BÃƒÂ»cheron", "<green>", "Hache"),
    FERMIER("Fermier", "<yellow>", "Houe"),
    CHASSEUR("Chasseur", "<red>", "Ãƒâ€°pÃƒÂ©e");

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
