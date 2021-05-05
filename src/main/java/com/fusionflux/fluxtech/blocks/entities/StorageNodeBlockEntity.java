package com.fusionflux.fluxtech.blocks.entities;

import com.fusionflux.fluxtech.blocks.FluxTechBlocks;
import com.fusionflux.fluxtech.blocks.inventory.ImplementedInventory;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ScheduledTick;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.CallbackI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class StorageNodeBlockEntity extends BlockEntity implements ImplementedInventory, Nameable {
    //private ItemStack item = ItemStack.EMPTY;
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
    private HashSet<BlockPos> connectedCore= new HashSet<>();

    public StorageNodeBlockEntity() {
        super(FluxTechBlocks.STORAGE_NODE_BLOCK_ENTITY);
    }

    @Override
    public void setLocation(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos.toImmutable();
    }

    @Override
    public void markRemoved() {
        if (this.world != null) {
            if (!this.world.isClient) {
                if (!this.removed) {
                    if (!connectedCore.isEmpty()) {
                        for (BlockPos cores : this.connectedCore) {
                            StorageCoreBlockEntity core;
                            core = (StorageCoreBlockEntity) this.world.getBlockEntity(cores);
                            if (core != null) {
                                core.onDelete(this.getPos());
                            }
                        }
                    }
                }
            }
        }
        this.removed = true;
    }



    public void checkConnections() {
        StorageCoreBlockEntity core;
        StorageNodeBlockEntity node;
        if (this.world != null) {
            if (!this.world.isClient) {
                for (Direction offsetdir : Direction.values()) {
                    if (this.world.getBlockState(this.getPos().offset(offsetdir)).getBlock().equals(FluxTechBlocks.STORAGE_CORE_BLOCK)) {
                        core = (StorageCoreBlockEntity) this.world.getBlockEntity(new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()).offset(offsetdir));
                        connectedCore.add(core.getPos());
                        core.addNewNodes(this.pos);
                    } else if (this.world.getBlockState(this.getPos().offset(offsetdir)).getBlock().equals(FluxTechBlocks.STORAGE_NODE_BLOCK)) {
                        node = (StorageNodeBlockEntity) this.world.getBlockEntity(new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()).offset(offsetdir));
                        if (node != null) {
                            if (!this.connectedCore.containsAll(node.connectedCore)) {
                                this.connectedCore.addAll(node.connectedCore);
                                for (BlockPos cores : node.connectedCore) {
                                    core = (StorageCoreBlockEntity) this.world.getBlockEntity(cores);
                                    if (core != null) {
                                        core.addNewNodes(this.pos);
                                    }
                                }
                                System.out.println("CHECKCONNECTIONS");
                            }
                        }
                    }
                }
                if (!connectedCore.isEmpty()) {
                    updatenearbyblocks();
                }
            }
        }
    }


    public void updatenearbyblocks() {
        StorageCoreBlockEntity core;
        StorageNodeBlockEntity node;
        for (Direction offsetdir : Direction.values()) {
            if (this.world.getBlockState(this.getPos().offset(offsetdir)).getBlock().equals(FluxTechBlocks.STORAGE_NODE_BLOCK)) {
                node = (StorageNodeBlockEntity) this.world.getBlockEntity(new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()).offset(offsetdir));
                if (node != null) {
                    if (!node.connectedCore.containsAll(this.connectedCore)) {

                        node.connectedCore.addAll(this.connectedCore);

                        for (BlockPos cores : this.connectedCore) {
                            core = (StorageCoreBlockEntity) this.world.getBlockEntity(cores);
                            if (core != null) {
                                core.addNewNodes(node.pos);
                            }
                        }
                        node.updatenearbyblocks();
                    }
                        System.out.println("UPDATENEARBYBLOCKS");
                }
            }
        }
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);

        Inventories.fromTag(tag, items);

        ListTag cores = tag.getList("cores", NbtType.COMPOUND);
        for(Tag name :cores){
            CompoundTag ctag = (CompoundTag) name;
            connectedCore.add(new BlockPos(ctag.getInt("corex"),
                    ctag.getInt("corey"),
                    ctag.getInt("corez")));
        }

    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        ListTag cores = new ListTag();

        for (BlockPos pos : connectedCore) {
            CompoundTag listTag = new CompoundTag();
            listTag.putInt("corex", pos.getX());
            listTag.putInt("corey", pos.getY());
            listTag.putInt("corez", pos.getZ());
            cores.add(listTag);
        }
        tag.put("cores",cores);

        Inventories.toTag(tag, items);

        return tag;
    }


    @Override
    public Text getName() {
        return new TranslatableText("container.node");
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    public void removeStoredBlockPos(BlockPos corePos){
        connectedCore.remove(corePos);
    }

    public void setConnectedCore() {
        this.connectedCore.clear();
    }

}
