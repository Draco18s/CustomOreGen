package CustomOreGen.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import CustomOreGen.Server.ConfigOption.DisplayGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * The "Custom Ore Generation..." screen opened from Create World: one tab per display group, and a
 * scrolling list of that group's options. Changing a choice whose display is dynamic reloads the config
 * (refreshGui = 2) so options it shows or hides appear; Defaults reloads it from scratch (refreshGui = 3).
 */
public class GuiCustomOreGenSettings extends Screen
{
	protected final Screen _parentGui;
	protected int refreshGui = 2;
	protected Button _doneButton = null;
	protected Button _resetButton = null;
	protected GuiCustomOreGenSettings.GuiOptionSlot _optionPanel = null;

	// Selecting a tab rebuilds the option list for its group.
	private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget,
			tab -> this.refreshGui = Math.max(this.refreshGui, 1), tab -> {});
	private MenuTabBar tabNavigationBar;

	public GuiCustomOreGenSettings(Screen parentGui)
	{
		super(Component.literal("Custom Ore Gen"));
		this._parentGui = parentGui;
	}

	@Override
	public void init()
	{
		super.init();
		super.clearWidgets();

		ConfigOption.DisplayGroup currentGroup = tabNavigationBar == null ? null : ((DisplayGroupTab)tabManager.getCurrentTab()).group;

		if (this.refreshGui >= 2)
		{
			if (this.refreshGui >= 3)
			{
				WorldConfig.loadedOptionOverrides[0] = null;
			}

			WorldConfig visibleGroups = null;

			while (visibleGroups == null)
			{
				try
				{
					visibleGroups = new WorldConfig();
				}
				catch (Exception var7)
				{
					if (ServerState.onConfigError(var7))
					{
						visibleGroups = null;
						continue;
					}

					visibleGroups = WorldConfig.createEmptyConfig();
				}

				WorldConfig.loadedOptionOverrides[0] = new ArrayList<ConfigOption>();
				for (ConfigOption option : visibleGroups.getConfigOptions()) {
					if (option.getDisplayState() != null && option.getDisplayState() != ConfigOption.DisplayState.hidden)
					{
						WorldConfig.loadedOptionOverrides[0].add(option);
					}
				}

				if (currentGroup != null)
				{
					ConfigOption visibleOptions = visibleGroups.getConfigOption(currentGroup.getName());

					if (visibleOptions instanceof ConfigOption.DisplayGroup)
					{
						currentGroup = (ConfigOption.DisplayGroup)visibleOptions;
					}
				}
			}
		}

		this.refreshGui = 0;
		Vector<ConfigOption.DisplayGroup> visibleGroups1 = new Vector<DisplayGroup>();
		Vector<ConfigOption> visibleOptions1 = new Vector<ConfigOption>();

		MenuTabBar.Builder bld = MenuTabBar.builder(this.tabManager, this.width);

		nextOption: for (ConfigOption option : WorldConfig.loadedOptionOverrides[0]) {

			ConfigOption.DisplayGroup group;

			if (option instanceof ConfigOption.DisplayGroup)
			{
				for (group = currentGroup; checkGroup(group, currentGroup); group = group.getDisplayGroup())
				{
					if (group == null)
					{
						continue nextOption;
					}
				}

				visibleGroups1.add((ConfigOption.DisplayGroup)option);
				bld.addTab(new GuiCustomOreGenSettings.DisplayGroupTab((ConfigOption.DisplayGroup)option));
			}
			else
			{
				for (group = option.getDisplayGroup(); checkGroup(group, currentGroup); group = group.getDisplayGroup())
				{
					if (group == null)
					{
						continue nextOption;
					}
				}

				visibleOptions1.add(option);
			}
		}

		if(tabNavigationBar == null) {
			this.tabNavigationBar = bld.build();
			this.tabNavigationBar.selectTab(0, false);
		}
		this.addRenderableWidget(this.tabNavigationBar);
		int listTop = visibleGroups1.isEmpty() ? 16 : 40;
		this._optionPanel = this.addRenderableWidget(new GuiOptionSlot(Minecraft.getInstance(), listTop, super.height - 30, 25, visibleOptions1));
		// Rebuilt on every init so they follow the current screen size; clearWidgets() above removed the old ones.
		this._doneButton = this.addRenderableWidget(Button.builder(Component.literal("Done"), this::actionPerformed)
				.pos(super.width / 2 - 155, super.height - 24)
				.size(150, 20).build());
		this._resetButton = this.addRenderableWidget(Button.builder(Component.literal("Defaults"), this::actionPerformed)
				.pos(super.width / 2 +5, super.height - 24)
				.size(150, 20).build());
		this.repositionElements();
	}

	private boolean checkGroup(DisplayGroup group, DisplayGroup displayGroup) {
		if(group == null || displayGroup == null) return group != displayGroup;
		return !group.equals(displayGroup);
	}

	@Override
	public void repositionElements() {
		if (this.tabNavigationBar != null) {
			this.tabNavigationBar.arrangeElements(this.width);
			int i = this.tabNavigationBar.getRectangle().bottom();
			ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - i);
			this.tabManager.setTabArea(screenrectangle);
		}
	}

	protected void actionPerformed(Button button)
	{
		if (button == this._doneButton)
		{
			this.onClose();
		}
		else if (button == this._resetButton)
		{
			this.refreshGui = 3;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, "CustomOreGen Options", super.width / 2, tabNavigationBar.getRectangle().bottom() + 4, 0xFFFFFFFF);
	}

	// Widgets are rebuilt between frames, not while one is being drawn.
	@Override
	public void tick() {
		super.tick();
		if (this.refreshGui > 0)
		{
			this.init();
		}
	}

	// Escape and Done both return to the screen that opened us (Create World), not to no screen.
	@Override
	public void onClose() {
		super.minecraft.gui.setScreen(_parentGui);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return this.tabNavigationBar.keyPressed(event) || super.keyPressed(event);
	}

	private class DisplayGroupTab extends GridLayoutTab {
		public final DisplayGroup group;

		public DisplayGroupTab(DisplayGroup option) {
			super(Component.literal(option.getLocalizedDisplayName()));
			group = option;
		}
	}

	/** One row per option: its name on the left (hover for the description) and its control on the right. */
	public class GuiOptionSlot extends ContainerObjectSelectionList<GuiOptionSlot.OptionEntry> {

		public GuiOptionSlot(Minecraft instance, int y0, int y1, int slotHeight, Vector<ConfigOption> options) {
			super(instance, GuiCustomOreGenSettings.this.width, y1 - y0, y0, slotHeight);
			int controlX = 2 * width / 5 + 15;
			int controlW = width / 10 + 100;

			for (ConfigOption option : options)
			{
				if (option instanceof ChoiceOption choice)
				{
					this.addEntry(new OptionEntry(new GuiChoiceButton(controlX, 0, controlW, slotHeight - 6, choice)));
				}
				else if (option instanceof NumericOption numeric)
				{
					this.addEntry(new OptionEntry(new GuiNumericSlider(controlX, 0, controlW, slotHeight - 6, numeric)));
				}
			}
		}

		// Rows span the screen; the name and control are placed by screen position, not row position.
		@Override
		public int getRowWidth() {
			return this.width - 20;
		}

		class OptionEntry extends ContainerObjectSelectionList.Entry<OptionEntry> {
			private final IOptionControl control;

			OptionEntry(IOptionControl control) {
				this.control = control;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				ConfigOption option = this.control.getOption();
				String optionName = option.getLocalizedDisplayName();
				int nameW = font.width(optionName);
				int nameX = 2 * GuiOptionSlot.this.width / 5 - 15 - nameW;
				int nameY = this.getContentY() + 6;
				graphics.text(font, Component.literal(optionName), nameX, nameY, 0xFFFFFFFF);

				if (mouseX >= nameX && mouseX <= nameX + nameW && mouseY >= nameY && mouseY <= nameY + font.lineHeight)
				{
					graphics.setTooltipForNextFrame(Component.literal(option.getLocalizedDescription()), mouseX, mouseY);
				}

				AbstractWidget widget = this.control.getControl();
				widget.setY(this.getContentY() + 1);
				widget.extractRenderState(graphics, mouseX, mouseY, a);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.control.getControl());
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.control.getControl());
			}
		}

		class GuiNumericSlider extends AbstractSliderButton implements IOptionControl {
			private final NumericOption _numeric;

			public GuiNumericSlider(int x, int y, int w, int h, NumericOption option) {
				super(x, y, w, h, Component.literal(""), (double)option.getNormalizedDisplayValue());
				_numeric = option;
				applyValue();
			}

			@Override
			public ConfigOption getOption() {
				return _numeric;
			}

			@Override
			public AbstractWidget getControl() {
				return this;
			}

			@Override
			protected void updateMessage() {

			}

			@Override
			protected void applyValue() {
				this._numeric.setNormalizedDisplayValue(this.value);
				long prec = 6L;
				long base = (long)Math.pow(10.0D, (double)prec);

				for (long val = Math.round(this._numeric.getDisplayIncr() * (double)base); prec > 0L && val % 10L == 0L; --prec)
				{
					val /= 10L;
					base /= 10L;
				}

				super.setMessage(Component.literal(String.format("%." + prec + "f", new Object[] {Double.valueOf(this._numeric.getDisplayValue())})));
			}

		}

		class GuiChoiceButton extends AbstractButton implements IOptionControl {
			private final ChoiceOption _choice;
			private final int _maxWidth;

			public GuiChoiceButton(int x, int y, int w, int h, ChoiceOption choice) {
				super(x, y, w, h, Component.literal(choice.getLocalizedDisplayValue()));
				_choice = choice;
				_maxWidth = w;
				onValueChanged();
			}

			@Override
			public ConfigOption getOption() {
				return _choice;
			}

			@Override
			public AbstractWidget getControl() {
				return this;
			}

			@Override
			public void onPress(InputWithModifiers input) {
				this._choice.setValue(this._choice.nextPossibleValue());
				this.onValueChanged();
				refreshGui = this._choice.getDisplayState() == ConfigOption.DisplayState.shown_dynamic ? 2 : 1;
			}

			@Override
			protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
				this.extractDefaultSprite(graphics);
				this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
			}

			@Override
			protected void updateWidgetNarration(NarrationElementOutput output) {
				this.defaultButtonNarrationText(output);
			}

			protected void onValueChanged()
			{
				super.setMessage(Component.literal(_choice.getLocalizedDisplayValue()));
				int strWidth = super.getMessage() == null ? 0 : font.width(super.getMessage());
				super.width = Math.min(this._maxWidth, strWidth + 10);
			}

		}
	}

	public interface IOptionControl
	{
		ConfigOption getOption();

		AbstractWidget getControl();
	}
}
