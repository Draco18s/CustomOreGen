package CustomOreGen.Client;

import java.util.List;
import java.util.function.Consumer;

import CustomOreGen.Server.GuiCustomOreGenSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

/** Adds the "Custom Ore Generation..." button to Create World's More tab. */
public class CreateWorldScreenHooks
{
    private static Button _optionsGuiButton = null;

    public static void addOptionsButtonToGui(CreateWorldScreen gui, List<GuiEventListener> buttonList, Consumer<GuiEventListener> add)
    {
        if (gui == null)
        {
            _optionsGuiButton = null;
        }
        else
        {
            GuiCustomOreGenSettings settingsGui = new GuiCustomOreGenSettings(gui);
            
            var action = new Button.OnPress() {
                @Override
                public void onPress(Button b) {
                	gui.getMinecraft().gui.setScreen(settingsGui);
                }
            };
            
            _optionsGuiButton = Button.builder(Component.literal("Custom Ore Generation..."), action).build();
            
            Button button1 = _optionsGuiButton;
            
            if (!buttonList.contains(button1))
            {
            	int n = gui.children().size();
            	
            	GuiEventListener b = gui.children().get(n-1);
            	
            	button1.setX(b.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL));
            	button1.setY(b.getRectangle().bottom() + 1);
                add.accept(button1);
            }
        }
    }
    
    public static void updateOptionsButtonVisibility(CreateWorldScreen gui) {
    	net.minecraft.client.gui.components.tabs.TabNavigationBar tabBar = (net.minecraft.client.gui.components.tabs.TabNavigationBar)gui.renderables.get(0);
    	int j = 0;
    	for(GuiEventListener child : tabBar.children()) {
    		net.minecraft.client.gui.components.TabButton tb = ((net.minecraft.client.gui.components.TabButton)child);
    		if(tb.isFocused()) {
    			break;
    		}
    		j++;
    	}
    	
    	GuiEventListener b = gui.children().get(0);
    	
    	_optionsGuiButton.setX(b.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL) - _optionsGuiButton.getWidth()/2);
    	
    	b = gui.children().get(gui.children().size() - 1);
    	
    	_optionsGuiButton.setY(b.getRectangle().bottom() + 1);
        
    	//TabNavigationBar bar;
    	//bar.getFocused().getTabOrderGroup()
    	
    	_optionsGuiButton.visible = j == 1;//!(Boolean)ObfuscationReflectionHelper.getPrivateValue(CreateWorldScreen.class, gui, "inMoreLevelOptionsDisplay");
    }
}
