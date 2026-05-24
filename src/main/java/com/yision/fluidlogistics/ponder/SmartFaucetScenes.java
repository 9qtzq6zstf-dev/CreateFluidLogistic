package com.yision.fluidlogistics.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.foundation.ponder.element.BeltItemElement;
import com.yision.fluidlogistics.block.SmartFaucet.SmartFaucetBlock;
import com.yision.fluidlogistics.block.SmartFaucet.SmartFaucetBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class SmartFaucetScenes {

    public static final String SMART_FAUCET = "smart_faucet";

    public static void smartFaucet(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(SMART_FAUCET, "Filling Items with Smart Faucets");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos faucet1Pos = util.grid().at(0, 2, 5);
        BlockPos tank1Bottom = util.grid().at(0, 1, 6);
        BlockPos tank1Top = util.grid().at(0, 2, 6);
        BlockPos depotPos = util.grid().at(0, 1, 5);
        BlockPos faucet2Pos = util.grid().at(2, 2, 2);
        BlockPos tank2Bottom = util.grid().at(2, 1, 3);
        BlockPos tank2Top = util.grid().at(2, 2, 3);
        BlockPos chestPos = util.grid().at(5, 2, 3);
        BlockPos funnelPos = util.grid().at(5, 2, 2);
        BlockPos depot1V = util.grid().at(3, 1, 3);

        Selection tank1Faucet1S = util.select().fromTo(tank1Bottom, tank1Top)
            .add(util.select().position(faucet1Pos));
        Selection depotS = util.select().position(depotPos);
        Selection faucet1S = util.select().position(faucet1Pos);
        Selection tank2S = util.select().fromTo(tank2Bottom, tank2Top);
        Selection faucet2S = util.select().position(faucet2Pos);
        Selection chestS = util.select().position(chestPos);
        Selection funnelS = util.select().position(funnelPos);
        Selection largeCog = util.select().position(7, 0, 3);
        Selection kinetics = util.select().position(6, 1, 3);
        Selection belt = util.select().fromTo(0, 1, 2, 6, 1, 2);

        ElementLink<WorldSectionElement> tank1Link =
            scene.world().showIndependentSection(tank1Faucet1S, Direction.DOWN);
        scene.world().moveSection(tank1Link, util.vector().of(3, 0, -2), 0);
        scene.idle(5);

        ElementLink<WorldSectionElement> depotLink =
            scene.world().showIndependentSection(depotS, Direction.DOWN);
        scene.world().moveSection(depotLink, util.vector().of(3, 0, -2), 0);
        scene.idle(15);

        scene.overlay()
            .showText(100)
            .text("Smart Faucets can fill items with fluids")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().of(3.5, 2.5, 3.5));
        scene.idle(70);

        ItemStack bucket = new ItemStack(Items.BUCKET);
        scene.world().createItemOnBeltLike(depotPos, Direction.NORTH, bucket);
        scene.idle(20);

        scene.world().modifyBlock(faucet1Pos,
            s -> s.setValue(SmartFaucetBlock.OPEN, true), false);

        CompoundTag waterTag = new CompoundTag();
        waterTag.putString("id", "minecraft:water");
        waterTag.putInt("amount", 1000);
        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> {
                nbt.put("RenderingFluid", waterTag);
                nbt.putInt("ProcessingTicks", 20);
                nbt.putBoolean("IsFillingItem", true);
                nbt.putInt("ProcessingTarget", 1);
            });
        scene.idle(25);

        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> nbt.remove("RenderingFluid"));
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, new ItemStack(Items.WATER_BUCKET));
        scene.idle(10);

        scene.overlay()
            .showText(80)
            .text("Filters can restrict which fluid types are allowed through")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().of(3.5, 2.78, 3.625));
        scene.idle(40);

        scene.overlay()
            .showFilterSlotInput(util.vector().of(3.5, 2.82, 3.625), Direction.UP, 60);
        scene.idle(10);

        scene.overlay()
            .showControls(util.vector().of(3.5, 2.78, 3.625), Pointing.DOWN, 60)
            .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.idle(50);

        scene.world().setFilterData(faucet1S, SmartFaucetBlockEntity.class, new ItemStack(Items.LAVA_BUCKET));
        scene.idle(20);

        scene.world().removeItemsFromBelt(depotPos);
        scene.world().createItemOnBeltLike(depotPos, Direction.NORTH, new ItemStack(Items.BUCKET));
        scene.idle(20);

        CompoundTag lavaTag = new CompoundTag();
        lavaTag.putString("id", "minecraft:lava");
        lavaTag.putInt("amount", 1000);
        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> {
                nbt.put("RenderingFluid", lavaTag);
                nbt.putInt("ProcessingTicks", 20);
                nbt.putBoolean("IsFillingItem", true);
                nbt.putInt("ProcessingTarget", 1);
            });
        scene.idle(25);

        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> nbt.remove("RenderingFluid"));
        scene.world().removeItemsFromBelt(depotPos);
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, new ItemStack(Items.LAVA_BUCKET));
        scene.idle(30);

        scene.world().hideIndependentSection(depotLink, Direction.NORTH);
        scene.world().setFilterData(faucet1S, SmartFaucetBlockEntity.class, ItemStack.EMPTY);
        scene.idle(20);

        scene.world().setBlock(depotPos, AllBlocks.BASIN.getDefaultState(), false);
        ElementLink<WorldSectionElement> basinLink =
            scene.world().showIndependentSection(depotS, Direction.SOUTH);
        scene.world().moveSection(basinLink, util.vector().of(3, 0, -2), 0);
        scene.idle(15);

        scene.overlay()
            .showText(60)
            .text("Smart Faucets can also fill some fluid containers")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().centerOf(depot1V));
        scene.idle(80);

        CompoundTag basinWaterTag = new CompoundTag();
        basinWaterTag.putString("id", "minecraft:water");
        basinWaterTag.putInt("amount", 10000);
        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> {
                nbt.put("RenderingFluid", basinWaterTag);
                nbt.putInt("ProcessingTicks", 20);
                nbt.putBoolean("IsFillingItem", true);
                nbt.putInt("ProcessingTarget", 0);
            });
        scene.idle(25);

        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> nbt.remove("RenderingFluid"));
        scene.world().modifyBlockEntity(depotPos, BasinBlockEntity.class, be -> {
            var fh = be.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
            if (fh != null)
                fh.fill(new FluidStack(Fluids.WATER, 10000), FluidAction.EXECUTE);
        });
        scene.idle(10);

        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> {
                nbt.put("RenderingFluid", lavaTag);
                nbt.putInt("ProcessingTicks", 20);
                nbt.putBoolean("IsFillingItem", true);
                nbt.putInt("ProcessingTarget", 0);
            });
        scene.idle(25);

        scene.world().modifyBlockEntityNBT(faucet1S, SmartFaucetBlockEntity.class,
            nbt -> nbt.remove("RenderingFluid"));
        scene.world().modifyBlockEntity(depotPos, BasinBlockEntity.class, be -> {
            var fh = be.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
            if (fh != null)
                fh.fill(new FluidStack(Fluids.LAVA, 10000), FluidAction.EXECUTE);
        });

        scene.idle(50);

        scene.world().hideIndependentSection(basinLink, Direction.UP);
        scene.world().hideIndependentSection(tank1Link, Direction.UP);
        scene.idle(5);
        scene.world().setBlock(depotPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        scene.idle(30);

        Selection scaffoldingS = util.select().position(5, 1, 3);

        scene.world().showSection(largeCog, Direction.UP);
        scene.world().showSection(kinetics, Direction.DOWN);
        scene.world().showSection(scaffoldingS, Direction.DOWN);
        scene.world().showSection(belt, Direction.SOUTH);
        scene.idle(5);
        scene.world().showSection(funnelS, Direction.DOWN);
        scene.world().showSection(chestS, Direction.DOWN);
        scene.idle(10);

        scene.world().modifyBlock(faucet2Pos,
            s -> s.setValue(SmartFaucetBlock.OPEN, true), false);

        ElementLink<WorldSectionElement> faucet2Link =
            scene.world().showIndependentSection(tank2S, Direction.DOWN);
        scene.idle(5);
        scene.world().showSectionAndMerge(faucet2S, Direction.SOUTH, faucet2Link);
        scene.idle(15);

        scene.overlay()
            .showText(80)
            .text("Smart Faucets can also fill items on belts")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().of(2.5, 2.5, 2.5));
        scene.idle(90);

        ItemStack bucket1 = new ItemStack(Items.BUCKET);
        ElementLink<BeltItemElement> bucketItem =
            scene.world().createItemOnBelt(util.grid().at(5, 1, 2), Direction.SOUTH, bucket1);
        scene.idle(45);

        scene.world().stallBeltItem(bucketItem, true);
        scene.idle(10);

        CompoundTag waterTag2 = new CompoundTag();
        waterTag2.putString("id", "minecraft:water");
        waterTag2.putInt("amount", 1000);
        scene.world().modifyBlockEntityNBT(faucet2S, SmartFaucetBlockEntity.class,
            nbt -> {
                nbt.put("RenderingFluid", waterTag2);
                nbt.putInt("ProcessingTicks", 20);
                nbt.putBoolean("IsFillingItem", true);
                nbt.putInt("ProcessingTarget", 2);
            });
        scene.idle(25);

        scene.world().modifyBlockEntityNBT(faucet2S, SmartFaucetBlockEntity.class,
            nbt -> nbt.remove("RenderingFluid"));
        scene.world().removeItemsFromBelt(faucet2Pos.below());
        bucketItem = scene.world().createItemOnBelt(faucet2Pos.below(), Direction.UP, new ItemStack(Items.WATER_BUCKET));
        scene.world().stallBeltItem(bucketItem, true);
        //scene.idle(5);
        scene.world().stallBeltItem(bucketItem, false);
        scene.idle(40);

        scene.overlay()
            .showText(70)
            .text("Without a filter, Smart Faucets choose the fluid intelligently based on the item")
            .placeNearTarget()
            .pointAt(util.vector().of(2.5, 2.5, 2.5));
        scene.idle(80);

        ItemStack powderedObsidian = AllItems.POWDERED_OBSIDIAN.asStack();
        ElementLink<BeltItemElement> obsidianItem =
            scene.world().createItemOnBelt(util.grid().at(5, 1, 2), Direction.SOUTH, powderedObsidian);
        scene.idle(45);

        scene.world().stallBeltItem(obsidianItem, true);
        scene.idle(10);

        CompoundTag lavaTag2 = new CompoundTag();
        lavaTag2.putString("id", "minecraft:lava");
        lavaTag2.putInt("amount", 1000);
        scene.world().modifyBlockEntityNBT(faucet2S, SmartFaucetBlockEntity.class,
            nbt -> {
                nbt.put("RenderingFluid", lavaTag2);
                nbt.putInt("ProcessingTicks", 20);
                nbt.putBoolean("IsFillingItem", true);
                nbt.putInt("ProcessingTarget", 2);
            });
        scene.idle(25);

        scene.world().modifyBlockEntityNBT(faucet2S, SmartFaucetBlockEntity.class,
            nbt -> nbt.remove("RenderingFluid"));
        scene.world().removeItemsFromBelt(faucet2Pos.below());
        obsidianItem = scene.world().createItemOnBelt(faucet2Pos.below(), Direction.UP, AllItems.INCOMPLETE_REINFORCED_SHEET.asStack());
        scene.world().stallBeltItem(obsidianItem, true);
        scene.world().stallBeltItem(obsidianItem, false);

        scene.markAsFinished();
    }
}
