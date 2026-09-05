package fr.gens.core.utils;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;

public class BedrockFormManager {

    private static String clean(String text) {
        if (text == null) return null;
        text = text.replaceAll("[\\uE000-\\uF8FF]", "");
        text = text.replaceAll("(?i)<bold>", "");
        text = text.replaceAll("(?i)[§&]l", ""); // Retire le gras
        
        // Les emojis non supportés par Minecraft (✧, ⭐, ▶, etc.) forcent Bedrock à utiliser
        // la police système "lisse" (Noto Sans) qui a l'air d'un bug pour les joueurs.
        // On remplace les emojis courants par des caractères normaux pour garder la police pixelisée.
        text = text.replace("✧", "*")
                   .replace("✦", "*")
                   .replace("⭐", "*")
                   .replace("▶", ">")
                   .replace("»", ">")
                   .replace("«", "<")
                   .replace("❤", "<3");
                   
        // Retire tous les autres caractères non-standards (sauf les accents et les codes couleurs)
        text = text.replaceAll("[^\\p{ASCII}§éàèùâêîôûäëïöüçÇÉÀÈÂÊÎÔÛÄËÏÖÜ\\n]", "");
        
        return text;
    }

    public interface BedrockButtonAction {
        void onClick(Player player);
    }

    public static class BedrockButton {
        private final String text;
        private final Material icon;
        private final String iconUrl;
        private final BedrockButtonAction action;

        public BedrockButton(String text, Material icon, BedrockButtonAction action) {
            this.text = text;
            this.icon = icon;
            this.iconUrl = null;
            this.action = action;
        }

        public BedrockButton(String text, String iconUrl, BedrockButtonAction action) {
            this.text = text;
            this.icon = null;
            this.iconUrl = iconUrl;
            this.action = action;
        }

        public String getText() { return text; }
        public Material getIcon() { return icon; }
        public String getIconUrl() { return iconUrl; }
        public BedrockButtonAction getAction() { return action; }
    }

    public static void openSimpleForm(Player player, String title, String content, List<BedrockButton> buttons) {
        if (!FloodgateUtil.isFloodgateInstalled()) return;

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(clean(title))
                .content(clean(content));

        for (BedrockButton btn : buttons) {
            String url = btn.getIconUrl() != null ? btn.getIconUrl() : getIconUrl(btn.getIcon());
            if (url != null) {
                builder.button(clean(btn.getText()), FormImage.Type.URL, url);
            } else {
                builder.button(clean(btn.getText()));
            }
        }

        builder.validResultHandler((form, response) -> {
            int clickedButtonId = response.clickedButtonId();
            if (clickedButtonId >= 0 && clickedButtonId < buttons.size()) {
                BedrockButton btn = buttons.get(clickedButtonId);
                if (btn.getAction() != null) {
                    // Cumulus callbacks execute on the main server thread — direct call is safe
                    btn.getAction().onClick(player);
                }
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    private static String getIconUrl(Material material) {
        if (material == null) return null;
        String name = material.name().toLowerCase();
        
        if (material == Material.PLAYER_HEAD) return null; // Les têtes n'ont pas d'icône web simple
        if (material == Material.CREEPER_HEAD) return "https://crafthead.net/helm/MHF_Creeper.png";
        if (material == Material.ZOMBIE_HEAD) return "https://crafthead.net/helm/MHF_Zombie.png";
        if (material == Material.SKELETON_SKULL) return "https://crafthead.net/helm/MHF_Skeleton.png";
        if (material == Material.WITHER_SKELETON_SKULL) return "https://crafthead.net/helm/MHF_Wither.png";
        if (material == Material.BARRIER) return null;
        if (material == Material.ENCHANTED_BOOK) name = "book"; // Fallback sur livre normal
        if (material == Material.TNT) name = "tnt_side";
        if (name.equals("chest")) {
            return "https://assets.mcasset.cloud/1.20.4/assets/minecraft/textures/block/barrel_side.png"; // Fallback car le coffre est une entité 3D
        }
        if (name.contains("wind_charge")) {
            return "https://assets.mcasset.cloud/1.20.4/assets/minecraft/textures/item/fire_charge.png";
        }

        // Some items are blocks, which have a different texture path
        if (material.isBlock()) {
            return "https://assets.mcasset.cloud/1.20.4/assets/minecraft/textures/block/" + name + ".png";
        }
        
        return "https://assets.mcasset.cloud/1.20.4/assets/minecraft/textures/item/" + name + ".png";
    }
}
