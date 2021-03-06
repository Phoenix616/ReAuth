package technicianlp.reauth;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;

public class GuiAccountList extends GuiScreen {

    private final static int BUTTON_WIDTH = 308;

    private GuiScreen parentScreen;

    private GuiButton loginButton;
    private GuiButton cancelButton;
    private GuiButton addButton;
    private GuiButton editButton;
    private GuiButton removeButton;

    private Secure.Account selectedAccount = null;
    private GuiSlotAccounts accountList;

    public GuiAccountList(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        super.initGui();

        int start = width / 2 - BUTTON_WIDTH / 2;
        int halfWidth = (BUTTON_WIDTH / 2 - 4);
        int thirdWidth = (BUTTON_WIDTH / 3 - 4);
        addButton(loginButton = new GuiButton(0, start, height - 50, halfWidth, 20, "Login"));
        addButton(addButton = new GuiButton(1, start + BUTTON_WIDTH - halfWidth, height - 50, halfWidth, 20, "Add Account"));
        addButton(editButton = new GuiButton(2, start, height - 27, thirdWidth, 20, "Edit account"));
        addButton(removeButton = new GuiButton(3, width / 2 - thirdWidth / 2, height - 27, thirdWidth, 20, "Remove account"));
        addButton(cancelButton = new GuiButton(4, start + BUTTON_WIDTH - thirdWidth, height - 27, thirdWidth, 20, I18n.format("gui.cancel")));
        if (Secure.accounts.isEmpty()) {
            loginButton.enabled = false;
            editButton.enabled = false;
            removeButton.enabled = false;
        } else {
            selectedAccount = Secure.accounts.values().iterator().next();
        }

        accountList = new GuiSlotAccounts(mc, width, height, 50, height - 60, 38);

        Secure.initSkinStuff();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        accountList.handleMouseInput();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        accountList.drawScreen(mouseX, mouseY, partialTicks);
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRenderer, "Account List", width / 2, 10, 0xffffff);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                if (selectedAccount.getPassword() == null) {
                    mc.displayGuiScreen(new GuiLogin(parentScreen, this, selectedAccount));
                } else {
                    try {
                        Secure.login(selectedAccount.getUsername(), selectedAccount.getPassword(), true);
                        mc.displayGuiScreen(parentScreen);
                    } catch (AuthenticationException e) {
                        mc.displayGuiScreen(new GuiErrorScreen("ReAuth", "Authentication Failed"));
                    }
                }
                break;
            case 1:
                mc.displayGuiScreen(new GuiLogin(parentScreen, this));
                break;
            case 2:
                Secure.accounts.remove(selectedAccount.getUsername());
                if (Secure.accounts.isEmpty())
                    mc.displayGuiScreen(parentScreen);
                else
                    selectedAccount = Secure.accounts.values().iterator().next();
                LiteModReAuth.saveConfig();
                break;
            case 3:
                mc.displayGuiScreen(new GuiLogin(parentScreen, this, selectedAccount));
                break;
            case 4:
                mc.displayGuiScreen(parentScreen);
                break;
        }
    }

    private class GuiSlotAccounts extends GuiSlot {

        public GuiSlotAccounts(Minecraft mcIn, int width, int height, int topIn, int bottomIn, int slotHeightIn) {
            super(mcIn, width, height, topIn, bottomIn, slotHeightIn);
        }

        @Override
        protected int getSize() {
            return Secure.accounts.size();
        }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            int i = 0;
            for (Secure.Account account : Secure.accounts.values()) {
                if (i == slotIndex) {
                    selectedAccount = account;
                    break;
                }
                i++;
            }
            if (isDoubleClick) {
                GuiAccountList.this.actionPerformed(loginButton);
            }
        }

        @Override
        protected boolean isSelected(int slotIndex) {
            int i = 0;
            for (Secure.Account account : Secure.accounts.values()) {
                if (i == slotIndex)
                    return selectedAccount.equals(account);
                i++;
            }
            return false;
        }

        @Override
        protected void drawBackground() {
            drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn,
                                float partialTicks) {
            Secure.Account account = null;
            int i = 0;
            for (Secure.Account a : Secure.accounts.values()) {
                if (i == slotIndex) {
                    account = a;
                    break;
                }
                i++;
            }

            if (account == null) {
                return;
            }

            drawString(fontRenderer, account.getDisplayName(), xPos + 50, yPos + 7, 0xffffff);
            drawString(fontRenderer, account.getUsername(), xPos + 50, yPos + 19, 0x777777);

            GameProfile gameProfile = new GameProfile(account.getUuid(), account.getDisplayName());
            if (account.getLastQuery() + 10 * 60 * 1000 < System.currentTimeMillis()) {
                if (!gameProfile.getProperties().containsKey("textures") || !gameProfile.isComplete()) {
                    gameProfile = TileEntitySkull.updateGameprofile(gameProfile);
                    if (account.getUuid() == null) {
                        account.setUuid(gameProfile.getId());
                        LiteModReAuth.saveConfig();
                    }
                    account.setLastQuery(System.currentTimeMillis());
                }
            }
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> profileTextures = Minecraft.getMinecraft()
                    .getSkinManager().loadSkinFromCache(gameProfile);
            ResourceLocation skinLocation;
            if (profileTextures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                skinLocation = Minecraft.getMinecraft().getSkinManager().loadSkin(
                        profileTextures.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            } else {
                UUID id = EntityPlayer.getUUID(gameProfile);
                skinLocation = DefaultPlayerSkin.getDefaultSkin(id);
            }

            Minecraft.getMinecraft().getTextureManager().bindTexture(skinLocation);
            drawScaledCustomSizeModalRect(xPos + 1, yPos + 1, 8, 8, 8, 8, 32, 32, 64, 64);
        }

    }

}
