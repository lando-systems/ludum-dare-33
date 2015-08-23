package lando.systems.ld33;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import lando.systems.ld33.accessors.RectangleAccessor;
import lando.systems.ld33.dialogue.Dialogue;
import lando.systems.ld33.entities.*;
import lando.systems.ld33.utils.Assets;

import java.util.Iterator;

/**
 * Created by dsgraham on 8/22/15.
 */
public class World {

    public static final float MAP_UNIT_SCALE    = 1f / 16f;
    public static final int   SCREEN_TILES_WIDE = 20;
    public static final int   SCREEN_TILES_HIGH = 15;
    public static final int     PIXELS_PER_TILE = Config.width / SCREEN_TILES_WIDE;

    public enum Phase {
        First, Second, Third
    }

    public TiledMapTileLayer          foregroundLayer;
    public TiledMapTileLayer          backgroundLayer;
    public TiledMap                   map;
    public OrthogonalTiledMapRenderer mapRenderer;
    public Array<Rectangle>           tileRects;
    public Pool<Rectangle>            rectPool;
    public Array<ObjectBase>          mapObjects;
    public OrthographicCamera         camera;
    public Array<EntityBase>          gameEntities;
    public PlayerGoomba               player;
    public float                      cameraLeftEdge;
    public float                      cameraRightEdge;
    public float                      gameWidth;
    public Phase                      phase;
    public int                        segment;
    public boolean                    done;
    public Dialogue                   dialogue;

    public World(OrthographicCamera cam, Phase p, SpriteBatch batch) {
        phase = p;
        done = false;
        dialogue = new Dialogue();

        gameEntities = new Array<EntityBase>();
        camera = cam;

        initPhase();
        mapRenderer = new OrthogonalTiledMapRenderer(map, MAP_UNIT_SCALE, batch);

        foregroundLayer = (TiledMapTileLayer) map.getLayers().get("foreground");
        backgroundLayer = (TiledMapTileLayer) map.getLayers().get("background");

        gameWidth = backgroundLayer.getWidth();
        cameraLeftEdge = SCREEN_TILES_WIDE / 2;
        cameraRightEdge = gameWidth - cameraLeftEdge;

        tileRects = new Array<Rectangle>();
        rectPool = Pools.get(Rectangle.class);

        gameEntities.add(player);
    }

    private void initPhase() {
        segment = 0;
        final TmxMapLoader mapLoader = new TmxMapLoader();

        switch (phase) {
            case First:
                Array<String> messages = new Array<String>();
                messages.add("\"You're late! Move left and get into position!\"");
                dialogue.show(1, 10, 18, 4, messages);

                // TODO: encapsulate map loading so that loadObjects is always called right after map load

                map = mapLoader.load("maps/level1.tmx");
                loadObjects();

                player = new PlayerGoomba(this, new Vector2(33.5f, 3f));
                player.canJump = false;
                player.canRight = false;
                player.moveDelay = EntityBase.PIPEDELAY;
                Tween.to(player.getBounds(), RectangleAccessor.Y, EntityBase.PIPEDELAY)
                     .target(player.getBounds().y + 1f)
                     .ease(Linear.INOUT)
                     .start(LudumDare33.tween);
                break;
            case Second:
                Gdx.gl.glClearColor(Assets.NIGHT_SKY_R, Assets.NIGHT_SKY_G, Assets.NIGHT_SKY_B, 1f);

                map = mapLoader.load("maps/enterhome.tmx");
                loadObjects();

                messages = new Array<String>();
                messages.add(Assets.playerName + ":\"Damn it I'm late getting home again.\"");
                dialogue.show(1, 10, 18, 4, messages);

                player = new PlayerGoomba(this, new Vector2(17, 2));
                player.canJump = false;
                player.canRight = false;
                player.setWounded();
                player.moveDelay = EntityBase.PIPEDELAY;
                Tween.to(player.getBounds(), RectangleAccessor.X, EntityBase.PIPEDELAY)
                     .target(player.getBounds().x - 1f)
                     .ease(Linear.INOUT)
                     .start(LudumDare33.tween);
                break;
            case Third:
                Gdx.gl.glClearColor(0f, 0f, 0f, 1f);

                map = mapLoader.load("maps/inhome-bedroom.tmx");
                loadObjects();

                messages = new Array<String>();
                messages.add(Assets.wifeName + ":\"What the hell, injured on the job again?! That's it, I'm taking the kids and going to my mother's house!\"");
                messages.add(Assets.playerName + "\"... wait, but... I ... don't go!\"");
                dialogue.show(1, 10, 18, 4, messages);

                player = new PlayerGoomba(this, new Vector2(20, 2));
                player.canJump = false;
                player.canRight = false;
                player.setWounded();
                player.moveDelay = EntityBase.PIPEDELAY;
                Tween.to(player.getBounds(), RectangleAccessor.X, EntityBase.PIPEDELAY)
                     .target(player.getBounds().x - 1f)
                     .ease(Linear.INOUT)
                     .start(LudumDare33.tween);
                break;

        }
    }

    public void update(float dt) {

        dialogue.update(dt);

        Iterator<EntityBase> iterator = gameEntities.iterator();
        while(iterator.hasNext()) {
            EntityBase entity = iterator.next();
            entity.update(dt);
            if (entity.dead)
                iterator.remove();
        }

        for (ObjectBase object : mapObjects) {
            object.update(dt);
        }

        handlePhaseUpdate(dt);

        float playerX = player.getBounds().x;
        if (playerX < camera.position.x - 3) camera.position.x = playerX + 3;
        if (playerX > camera.position.x + 2) camera.position.x = playerX - 2;

        // keep the map in view always
        camera.position.x = Math.min(cameraRightEdge, Math.max(cameraLeftEdge, camera.position.x));
        camera.update();
    }


    public boolean allowPolling(){
        return !dialogue.isActive();
    }

    public void getTiles (int startX, int startY, int endX, int endY, Array<Rectangle> tiles) {
        rectPool.freeAll(tiles);
        tiles.clear();
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                TiledMapTileLayer.Cell cell = foregroundLayer.getCell(x, y);
                if (cell != null) {
                    Rectangle rect = rectPool.obtain();
                    rect.set(x, y, 1, 1);
                    tiles.add(rect);
                }
            }
        }
    }

    public Array<ObjectBase> getObjects(){
        return mapObjects;
    }

    public void render(SpriteBatch batch){

        mapRenderer.setView(camera);

        batch.begin();
        batch.setProjectionMatrix(camera.combined);

        mapRenderer.renderTileLayer(backgroundLayer);

        for (EntityBase entity : gameEntities){
            entity.render(batch);
        }
        for (ObjectBase object : mapObjects) {
            object.render(batch);
        }

        mapRenderer.renderTileLayer(foregroundLayer);

        batch.end();

    }

    public void renderUI(SpriteBatch batch) {
        dialogue.render(batch);
    }

    // ------------------------------------------------------------------------
    // Private Implementation
    // ------------------------------------------------------------------------


    private void handlePhaseUpdate(float dt){
        switch (phase){
            case First:
                switch (segment){
                    case 0:
                        if (player.getBounds().x < 27){
                            player.getBounds().x = 27;
                            segment++;
                            player.moveDelay = 6;

                            Array<String> messages = new Array<String>();
                            messages.add("\"All right everyone! Here we go!\"");
                            dialogue.show(1,10,18,4,messages);

                            MarioAI mario = new MarioAI(this, new Vector2(10, 2));
                            gameEntities.add(mario);
                        }
                        break;
                    case 1:
                        if (player.moveDelay <= 0){
                            segment++;
                            Array<String> messages = new Array<String>();
                            messages.add("\"Good Work!  Head on home to your family.\"");
                            dialogue.show(1, 10, 18, 4, messages);
                            player.setWounded();
                        }
                        break;
                    case 2:

                        if (player.getBounds().x <= 5.5){
                            Tween.to(player.getBounds(), RectangleAccessor.X, EntityBase.PIPEDELAY)
                                    .target(3.5f)
                                    .ease(Linear.INOUT)
                                    .setCallback(new TweenCallback() {
                                        @Override
                                        public void onEvent(int i, BaseTween<?> baseTween) {
                                            World.this.done = true;
                                        }
                                    })
                                    .start(LudumDare33.tween);
                        }
                }
                break;
            case Second:
                switch (segment){
                    case 0:
                        if (player.getBounds().x < 16){
                            player.getBounds().x = 16;
                            segment++;
                        }
                        break;
                    case 1:
                        if (player.getBounds().x < 10) {
                            player.getBounds().x = 10;
                            segment++;
                            player.moveDelay = EntityBase.PIPEDELAY;
                            Tween.to(player.getBounds(), RectangleAccessor.X, EntityBase.PIPEDELAY)
                                 .target(8f)
                                 .ease(Linear.INOUT)
                                 .setCallback(new TweenCallback() {
                                     @Override
                                     public void onEvent(int i, BaseTween<?> baseTween) {
                                         World.this.done = true;
                                     }
                                 })
                                 .start(LudumDare33.tween);
                        }
                        break;
                }
                break;
            case Third:
                switch (segment) {
                    case 0:
                        if (player.getBounds().x < 19){
                            player.getBounds().x = 19;
                            segment++;
                        }
                        break;
                    // TODO: wife walks out with kids
                    case 1:
                        if (player.getBounds().x < 9) {
                            player.getBounds().x = 9;
                            segment++;
                            player.moveDelay = EntityBase.PIPEDELAY;
                            Tween.to(player.getBounds(), RectangleAccessor.Y, EntityBase.PIPEDELAY)
                                 .target(player.getBounds().y + 1f)
                                 .ease(Linear.INOUT)
                                 .setCallback(new TweenCallback() {
                                     @Override
                                     public void onEvent(int i, BaseTween<?> baseTween) {
                                         World.this.done = true;
                                     }
                                 })
                                 .start(LudumDare33.tween);
                        }
                        break;
                }
        }
    }

    private void loadObjects() {
        if (map == null) return;

        mapObjects = new Array<ObjectBase>();

        MapProperties props;
        MapLayer objectLayer = map.getLayers().get("objects");
        for (MapObject object : objectLayer.getObjects()) {
            props = object.getProperties();
            float w = (Float) props.get("width");
            float h = (Float) props.get("height");
            float x = (Float) props.get("x");
            float y =((Float) props.get("y")) + h; // NOTE: god dammit... off by 1
            String type = (String) props.get("type");

            // Instantiate based on type
            if (type.equals("qblock")) {
                String dropTypeName = (String) props.get("drops");
                ItemEntity.ItemType dropType = ItemEntity.ItemType.getType(dropTypeName);
                mapObjects.add(new QuestionBlock(this, new Rectangle(x / w, y / h, 1, 1), dropType));
            }
//            else if (type.equals("...")) {
//
//            }
        }
    }

}
