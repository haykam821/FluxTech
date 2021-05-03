package com.fusionflux.fluxtech.blocks.entities;

import com.fusionflux.fluxtech.blocks.FluxTechBlocks;
import com.fusionflux.fluxtech.blocks.StorageNodeBlock;
import com.fusionflux.fluxtech.blocks.inventory.ImplementedInventory;
import com.fusionflux.fluxtech.blocks.inventory.MultiInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class StorageCoreBlockEntity extends BlockEntity implements ImplementedInventory, Nameable {

    public final List<BlockPos> connectedNodes = new ArrayList<>();
    private DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
    public StorageCoreBlockEntity() {
        super(FluxTechBlocks.STORAGE_CORE_BLOCK_ENTITY);
    }

    public void addNewNodes(BlockPos nodeBlockPos){
        if(this.world!=null) {
            if (!this.world.isClient) {
                connectedNodes.add(nodeBlockPos);
                StorageNodeBlockEntity node;
                List<Inventory> inventories = new ArrayList<>();
                for (BlockPos locker : connectedNodes) {
                    BlockEntity rawEntity = world.getBlockEntity(locker);
                    if(rawEntity instanceof StorageNodeBlockEntity){ // Also a null check
                        inventories.add((StorageNodeBlockEntity)rawEntity);
                    }
                }
                Inventory combined = new MultiInventory(inventories);
            }
        }
    }

    public void onDelete(BlockPos deletedLocker) {
        for (BlockPos locker : connectedNodes) {
            if (locker == deletedLocker) {
                connectedNodes.remove(deletedLocker);
                break;
            }
        }

        if (this.world != null) {
            if (!this.world.isClient) {
                //List<BlockPos> savedList = new ArrayList<>(connectedNodes);
                StorageNodeBlockEntity node;
                for (BlockPos locker : connectedNodes) {
                    node = (StorageNodeBlockEntity) this.world.getBlockEntity(locker);
                    if(node!=null) {
                        node.setConnectedCore();
                    }
                }

                connectedNodes.clear();

                for (Direction offsetdir : Direction.values()) {
                    if (this.world.getBlockState(this.getPos().offset(offsetdir)).getBlock().equals(FluxTechBlocks.STORAGE_NODE_BLOCK)) {
                        node = (StorageNodeBlockEntity) this.world.getBlockEntity(new BlockPos(this.getPos().getX(), this.getPos().getY(), this.getPos().getZ()).offset(offsetdir));
                        if(node!=null) {
                            node.checkConnections();
                        }

                        }
                }

                //savedList.clear();
            }
        }
    }



    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        Inventories.fromTag(tag, items);
        int size = tag.getInt("size");
        for (int i = 0; i < size; i++) {
            connectedNodes.add(new BlockPos(
                    tag.getInt(i + "nodex"),
                    tag.getInt(i + "nodey"),
                    tag.getInt(i + "nodez")
            ));
        }

    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        Inventories.toTag(tag,items);
        tag.putInt("size", connectedNodes.size());
        for (int i = 0; i < connectedNodes.size(); i++) {
            tag.putInt(i + "nodex", connectedNodes.get(i).getX());
            tag.putInt(i + "nodey", connectedNodes.get(i).getY());
            tag.putInt(i + "nodez", connectedNodes.get(i).getZ());
        }

        return tag;
    }

    @Override
    public Text getName() {
        return new TranslatableText("container.core");
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

}
