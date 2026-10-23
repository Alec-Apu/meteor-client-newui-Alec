package meteordevelopment.meteorclient.systems.modules.misc;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.*;
import meteordevelopment.meteorclient.utils.misc.Localization;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import java.util.List;

/** Native controls for the installed Baritone API. Never sends a chat command. */
public final class BaritoneControl extends Module {
    private final SettingGroup mine = settings.createGroup("Mining");
    public final Setting<List<Block>> ores = mine.add(new BlockListSetting.Builder().name("mining-blocks").description("Blocks to search for and mine.").defaultValue(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE).build());
    public final Setting<Integer> count = mine.add(new IntSetting.Builder().name("mining-count").description("Requested drop count; zero mines continuously.").defaultValue(0).min(0).noSlider().build());
    private final SettingGroup path = settings.createGroup("Navigation");
    public final Setting<Integer> x = path.add(new IntSetting.Builder().name("target-x").description("Destination X.").defaultValue(0).range(-29999984,29999984).noSlider().build());
    public final Setting<Integer> y = path.add(new IntSetting.Builder().name("target-y").description("Destination Y.").defaultValue(64).range(-2048,2048).noSlider().build());
    public final Setting<Integer> z = path.add(new IntSetting.Builder().name("target-z").description("Destination Z.").defaultValue(0).range(-29999984,29999984).noSlider().build());
    public final Setting<Boolean> ignoreY = path.add(new BoolSetting.Builder().name("ignore-target-y").description("Navigate to X/Z without requiring a specific elevation.").defaultValue(false).build());
    private final SettingGroup options = settings.createGroup("Travel Options");
    public final Setting<Boolean> breakBlocks = options.add(new BoolSetting.Builder().name("path-allow-break").description("Allow Baritone to break blocks while running panel tasks.").defaultValue(true).build());
    public final Setting<Boolean> placeBlocks = options.add(new BoolSetting.Builder().name("path-allow-place").description("Allow Baritone to place blocks while running panel tasks.").defaultValue(true).build());
    public final Setting<Boolean> sprint = options.add(new BoolSetting.Builder().name("path-allow-sprint").description("Allow Baritone to sprint while running panel tasks.").defaultValue(true).build());
    private WLabel status;
    private boolean owned, paused;
    private boolean oldBreak, oldPlace, oldSprint, appliedBreak, appliedPlace, appliedSprint;
    private String notice = "";
    public BaritoneControl() { super(Categories.Misc, "baritone", "Control Baritone mining and navigation in one panel."); runInMainMenu = true; }
    private IBaritone api() { return BaritoneAPI.getProvider().getPrimaryBaritone(); }
    private String tr(String value) { return Localization.text(value); }
    private boolean ready() {
        if (!isActive()) {notice = "Enable Baritone controls first.";return false;}
        if (mc.player == null || mc.level == null) {notice = "Join a server first.";return false;}
        if (Modules.get().isActive(InfinityMiner.class) || Modules.get().isActive(Excavator.class)) {notice = "Stop Infinity Miner or Excavator before starting a panel task.";return false;}
        if (!owned && (api().getPathingBehavior().isPathing() || api().getPathingBehavior().getInProgress().isPresent() || api().getPathingControlManager().mostRecentInControl().isPresent())) {notice = "Another Baritone task is active. Stop it explicitly before starting.";return false;}
        return true;
    }
    private void acquire() {
        if (owned) stopOwned();
        var settings = BaritoneAPI.getSettings();
        oldBreak=settings.allowBreak.value;oldPlace=settings.allowPlace.value;oldSprint=settings.allowSprint.value;
        appliedBreak=breakBlocks.get();appliedPlace=placeBlocks.get();appliedSprint=sprint.get();
        settings.allowBreak.value=appliedBreak;settings.allowPlace.value=appliedPlace;settings.allowSprint.value=appliedSprint;
        owned=true;paused=false;
    }
    public boolean startMining() {
        if (!ready()) return false;
        if (ores.get().isEmpty()) {notice="Select at least one mining block.";return false;}
        acquire();
        api().getMineProcess().mine(count.get(), ores.get().toArray(Block[]::new));
        notice="Mining requested. Check the live process state.";return true;
    }
    public boolean startPath() {
        if (!ready()) return false;
        if (!ignoreY.get() && (y.get()<mc.level.getMinY() || y.get()>=mc.level.getMaxY())) {notice="Destination Y is outside this dimension.";return false;}
        acquire();
        api().getCustomGoalProcess().setGoalAndPath(ignoreY.get()?new GoalXZ(x.get(),z.get()):new GoalBlock(new BlockPos(x.get(),y.get(),z.get())));
        notice="Navigation requested. Check the live process state.";return true;
    }
    public void pauseTask() {if(!owned || paused)return;PathManagers.get().pause();paused=true;}
    public void resumeTask() {if(!owned || !paused || !isActive())return;PathManagers.get().resume();paused=false;}
    private void restore() {
        if(!owned)return;var settings=BaritoneAPI.getSettings();
        if(settings.allowBreak.value.equals(appliedBreak))settings.allowBreak.value=oldBreak;
        if(settings.allowPlace.value.equals(appliedPlace))settings.allowPlace.value=oldPlace;
        if(settings.allowSprint.value.equals(appliedSprint))settings.allowSprint.value=oldSprint;
        owned=false;
    }
    private void stopOwned() {
        if(!owned)return;api().getPathingBehavior().cancelEverything();api().getInputOverrideHandler().clearAllKeys();
        if(paused)PathManagers.get().resume();paused=false;restore();
    }
    public void stopAll() {
        api().getPathingBehavior().cancelEverything();api().getInputOverrideHandler().clearAllKeys();
        if(paused)PathManagers.get().resume();paused=false;restore();notice="Stop requested.";
    }
    @Override public void onDeactivate() {stopOwned();}
    @EventHandler private void left(GameLeftEvent event) {stopOwned();}
    public String liveStatus() {
        var process=api().getPathingControlManager().mostRecentInControl();
        return tr(paused?"Paused":api().getPathingBehavior().isPathing()?"Pathing":api().getPathingBehavior().getInProgress().isPresent()?"Calculating path":"Idle / waiting") + (process.isPresent()?" | "+process.get().displayName():"");
    }
    public void tickWidget() {if(status!=null)status.set(liveStatus()+" | "+tr(notice));}
    private void button(WContainer parent,String name,Runnable action) {var b=parent.add(themeButton(parent,name)).widget();b.action=action;}
    private meteordevelopment.meteorclient.gui.widgets.pressable.WButton themeButton(WContainer parent,String name) {return parent.theme.button(tr(name));}
    @Override public WWidget getWidget(GuiTheme theme) {
        var panel=theme.verticalList();
        status=panel.add(theme.label(liveStatus())).widget();
        var mining=panel.add(theme.horizontalList()).widget();
        button(mining,"Start Mining",this::startMining);
        var navigation=panel.add(theme.horizontalList()).widget();
        button(navigation,"Start Navigation",this::startPath);
        button(navigation,"Use Current Position",()->{if(mc.player!=null){x.set(mc.player.blockPosition().getX());y.set(mc.player.blockPosition().getY());z.set(mc.player.blockPosition().getZ());if(mc.gui.screen() instanceof meteordevelopment.meteorclient.gui.WidgetScreen screen)screen.reload();}});
        var controls=panel.add(theme.horizontalList()).widget();
        button(controls,"Pause Task",this::pauseTask);button(controls,"Resume Task",this::resumeTask);
        button(panel,"Stop All Baritone Tasks",this::stopAll);
        return panel;
    }
}
