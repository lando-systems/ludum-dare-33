package lando.systems.ld33.entities;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Linear;
import com.badlogic.gdx.math.Vector2;
import lando.systems.ld33.LudumDare33;
import lando.systems.ld33.World;
import lando.systems.ld33.accessors.RectangleAccessor;
import lando.systems.ld33.utils.Assets;

/**
 * Brian Ploeckelman created on 8/22/2015.
 */
public class MushroomItem extends ItemEntity {

    public MushroomItem(World w, float px, float py) {
        super(w, px, py);
        keyframe = Assets.bigMushroom;
        Tween.to(bounds, RectangleAccessor.Y, ITEMDELAY)
             .target(py + 1.1f)
             .ease(Linear.INOUT)
             .start(LudumDare33.tween);
        moveDelay = ITEMDELAY+ .1f;
        velocity.x = 4;
    }

    @Override
    protected void hitHorizontal(){
        velocity.x = -velocity.x;
    }

}
