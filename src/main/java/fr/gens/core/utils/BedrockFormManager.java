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
        return text.replaceAll("[\\uE000-\\uF8FF]", "");
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
        
        // Some items are blocks, which have a different texture path
        if (material.isBlock()) {
            return "https://assets.mcasset.cloud/1.20.4/assets/minecraft/textures/block/" + name + ".png";
        }
        
        return "https://assets.mcasset.cloud/1.20.4/assets/minecraft/textures/item/" + name + ".png";
    }
}
