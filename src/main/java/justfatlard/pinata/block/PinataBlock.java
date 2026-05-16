package justfatlard.pinata.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PinataBlock extends BaseEntityBlock {
    public static final MapCodec<PinataBlock> CODEC = simpleCodec(PinataBlock::new);

    protected static final VoxelShape SHAPE = Block.box(3.0, 0.0, 1.0, 13.0, 10.0, 15.0);

    public PinataBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PinataBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void attack(BlockState state, Level world, BlockPos pos, Player player) {
        if (world.isClientSide()) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof PinataBlockEntity pinata) {
            pinata.onHit(world, pos, player);
        }
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        return 0.0f;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        return state;
    }

    public static void spawnHitParticles(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverWorld) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            serverWorld.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 3, 0.3, 0.3, 0.3, 0.05);
            serverWorld.sendParticles(ParticleTypes.NOTE, x, y + 0.6, z, 1, 0.2, 0.1, 0.2, 0.0);
        }
    }

    public static void spawnBurstParticles(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverWorld) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            serverWorld.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 3, 0.2, 0.2, 0.2, 0.0);
            serverWorld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.5, z, 40, 0.1, 0.1, 0.1, 0.7);
            serverWorld.sendParticles(ParticleTypes.NOTE, x, y + 0.5, z, 8, 0.5, 0.3, 0.5, 0.0);
            serverWorld.sendParticles(ParticleTypes.FIREWORK, x, y + 0.5, z, 15, 0.3, 0.3, 0.3, 0.3);
        }
    }

    public static void spawnCooldownParticles(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverWorld) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            serverWorld.sendParticles(ParticleTypes.SMOKE, x, y + 0.3, z, 3, 0.2, 0.2, 0.2, 0.01);
        }
    }

    public static void playHitSound(Level world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.7f, 1.0f + world.getRandom().nextFloat() * 0.4f);
    }

    public static void playCooldownSound(Level world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.5f, 0.8f);
    }

    public static void playBreakSound(Level world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
    }
}
