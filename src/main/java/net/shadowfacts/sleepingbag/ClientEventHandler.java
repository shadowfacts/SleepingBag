package net.shadowfacts.sleepingbag;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {

    public boolean needsToPop; //a variable to deal with popping the matrix when it should do so

    /**
     * Vanilla beds aren't full-height, so the player model moves down to accommodate that.
     * Moving the player up when using a sleeping bag visually "fixes" that change so that the player isn't sleeping inside of a block.
     * @param event - the render player event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof EntityPlayer) {                    //only care about players
            EntityPlayer playerEntity = (EntityPlayer) event.getEntity();
            if (playerEntity.isPlayerSleeping()) {                          //only care about sleeping players
                if (ItemSleepingBag.isWearingSleepingBag(playerEntity)) {   //only care about sleeping players specifically in sleeping bags
                    GlStateManager.pushMatrix();                            //push
                    GlStateManager.translate(0F, 0.24F, 0F);                //move the player back up onto the block
                    this.needsToPop = true;                                 //set it to pop the matrix
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRender(RenderPlayerEvent.Post event) {
        if (this.needsToPop) {
            this.needsToPop = false;
            GlStateManager.popMatrix();
        }
    }
}
