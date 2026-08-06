package fr.gens.core.modules;

public interface Module {
    String getName();
    String getDescription();
    boolean isEnabled();
    void enable();
    void disable();
}
