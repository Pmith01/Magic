package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.ItemData;
import forge.adventure.stage.ConsoleCommandInterpreter;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.util.Paths;

import java.util.HashMap;
import java.util.Map;

public class InventoryScene  extends UIScene {
    TextraButton leave;
    Button equipButton;
    TextraButton useButton;
    TextraLabel itemDescription;
    Dialog confirm;
    Dialog useDialog;
    private final Table inventory;
    private final Array<Button> inventoryButtons=new Array<>();
    private final HashMap<String,Button> equipmentSlots=new HashMap<>();
    HashMap<Button,String> itemLocation=new HashMap<>();
    Button selected;
    Button deleteButton;
    Texture equipOverlay;
    int columns=0;
    public InventoryScene()
    {
        super(Forge.isLandscapeMode() ? "ui/inventory.json" : "ui/inventory_portrait.json");
        equipOverlay = new Texture(Config.instance().getFile(Paths.ITEMS_EQUIP));
        ui.onButtonPress("return", () -> done());
        leave = ui.findActor("return");
        ui.onButtonPress("delete", () -> confirm.show(stage));
        ui.onButtonPress("equip", () -> equip());
        ui.onButtonPress("use", () -> use());
        equipButton = ui.findActor("equip");
        useButton= ui.findActor("use");
        useButton.setDisabled(true);
        deleteButton = ui.findActor("delete");
        itemDescription = ui.findActor("item_description");
        itemDescription.setAlignment(Align.topLeft);


        Array<Actor> children = ui.getChildren();
        for (int i = 0, n = children.size; i < n; i++)
        {

            if(children.get(i).getName()!=null&&children.get(i).getName().startsWith("Equipment"))
            {
                String slotName=children.get(i).getName().split("_")[1];
                equipmentSlots.put(slotName, (Button) children.get(i));
                Actor slot=children.get(i);
                slot.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Button button=((Button) actor);
                        if(button.isChecked())
                        {
                            for(Button otherButton:equipmentSlots.values())
                            {
                                if(button!=otherButton&&otherButton.isChecked()){
                                    otherButton.setChecked(false);
                                }
                            }
                            String item=Current.player().itemInSlot(slotName);
                            if(item!=null&& !item.equals(""))
                            {
                                Button changeButton=null;
                                for(Button invButton:inventoryButtons)
                                {
                                    if(itemLocation.get(invButton)!=null&&itemLocation.get(invButton).equals(item))
                                    {
                                        changeButton=invButton;
                                        break;
                                    }
                                }
                                if(changeButton!=null)
                                    changeButton.setChecked(true);
                            }
                            else
                            {
                                setSelected(null);
                            }
                        }

                    }
                });
            }
        }
        inventory = new Table(Controls.getSkin());
        ScrollPane scrollPane = ui.findActor("inventory");
        scrollPane.setScrollingDisabled(true,false);
        scrollPane.setActor(inventory);
        columns= (int) (scrollPane.getWidth()/createInventorySlot().getWidth());
        columns-=1;
        if(columns<=0)columns=1;
        scrollPane.setActor(inventory);
        confirm = new Dialog("", Controls.getSkin())
        {
            protected void result(Object object)
            {
                if(object!=null&&object.equals(true))
                    delete();
                confirm.hide();
            }
        };
        confirm.text( Controls.newLabel(Forge.getLocalizer().getMessage("lblDelete")));

        confirm.button(Forge.getLocalizer().getMessage("lblYes"), true);
        confirm.button(Forge.getLocalizer().getMessage("lblNo"), false);
        ui.addActor(confirm);
        confirm.hide();

        itemDescription.setWrap(true);
        //makes confirm dialog hidden immediately when you open inventory first time..
        confirm.getColor().a = 0;


        useDialog = new Dialog("", Controls.getSkin())
        {
            protected void result(Object object)
            {
                useDialog.hide();
                if(object!=null&&object.equals(true))
                {
                    triggerUse();
                    useDialog.getColor().a = 0;
                }
            }
        };

        useDialog.button(Forge.getLocalizer().getMessage("lblYes"), true);
        useDialog.button(Forge.getLocalizer().getMessage("lblNo"), false);
        ui.addActor(useDialog);
        useDialog.hide();
        useDialog.getColor().a = 0;
    }

    private static InventoryScene object;

    public static InventoryScene instance() {
        if(object==null)
            object=new InventoryScene();
        return object;
    }


    public void done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }

    public void delete() {

        ItemData data = ItemData.getItem(itemLocation.get(selected));
        if(data != null) {
            Current.player().removeItem(data.name);
        }
        updateInventory();

    }
    public void equip() {
        if(selected == null) return;
        ItemData data = ItemData.getItem(itemLocation.get(selected));
        if(data==null)return;
        Current.player().equip(data);
        updateInventory();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }


    private void triggerUse() {
        if(selected==null)return;

        ItemData data = ItemData.getItem(itemLocation.get(selected));
        if(data==null)return;
        Current.player().addMana(-data.manaNeeded);
        done();
        ConsoleCommandInterpreter.getInstance().command(data.commandOnUse);
    }
    private void use() {
        useDialog.getContentTable().clear();
        ItemData data = ItemData.getItem(itemLocation.get(selected));
        if(data==null)return;
        useDialog.getContentTable().add(Controls.newTextraLabel("Use "+data.name+"?\n"+data.getDescription()));
        useDialog.show(stage);
    }

    private void setSelected(Button actor) {
        selected=actor;
        if(actor==null)
        {
            itemDescription.setText("");
            deleteButton.setDisabled(true);
            equipButton.setDisabled(true);
            useButton.setDisabled(true);
            for(Button button:inventoryButtons)
            {
                button.setChecked(false);
            }
            return;
        }
        ItemData data = ItemData.getItem(itemLocation.get(actor));

        if(data==null) return;
        deleteButton.setDisabled(data.questItem);

        boolean isInPoi = MapStage.getInstance().isInMap();
        useButton.setDisabled(!(isInPoi&&data.usableInPoi||!isInPoi&&data.usableOnWorldMap));
        if(data.manaNeeded==0)
            useButton.setText("Use");
        else
            useButton.setText("Use "+data.manaNeeded+"[+Mana]");
        useButton.layout();
        if(Current.player().getMana()<data.manaNeeded)
            useButton.setDisabled(true);

        if(data.equipmentSlot==null|| data.equipmentSlot.equals(""))
        {
            equipButton.setDisabled(true);
        }
        else
        {
            equipButton.setDisabled(false);
            if(equipButton instanceof TextraButton)
            {
                TextraButton button=(TextraButton) equipButton;
                String item=Current.player().itemInSlot(data.equipmentSlot);
                if(item!=null&&item.equals(data.name))
                {
                    button.setText("Unequip");
                }
                else
                {
                    button.setText("Equip");
                }
                button.layout();
            }
        }

        for(Button button:inventoryButtons)
        {
            if(actor!=button&&button.isChecked()){
                button.setChecked(false);
            }
        }
        itemDescription.setText(data.name+"\n"+data.getDescription());


    }

    private void updateInventory() {
        clearActorObjects();
        inventoryButtons.clear();
        inventory.clear();
        for(int i=0;i<Current.player().getItems().size;i++) {

            if(i%columns==0)
                inventory.row();
            Button newActor=createInventorySlot();
            inventory.add(newActor).top().left().space(1);
            inventoryButtons.add(newActor);
            ItemData item=ItemData.getItem(Current.player().getItems().get(i));
            if(item==null)
            {
                System.err.print("Can not find item name "+Current.player().getItems().get(i)+"\n");
                continue;
            }
            if(item.sprite()==null)
            {
                System.err.print("Can not find sprite name "+item.iconName+"\n");
                continue;
            }
            Image img=new Image(item.sprite());
            img.setX((newActor.getWidth()-img.getWidth())/2);
            img.setY((newActor.getHeight()-img.getHeight())/2);
            newActor.addActor(img);
            itemLocation.put(newActor,Current.player().getItems().get(i));
            if(Current.player().getEquippedItems().contains(item.name))
            {
                Image overlay=new Image(equipOverlay);
                overlay.setX((newActor.getWidth()-img.getWidth())/2);
                overlay.setY((newActor.getHeight()-img.getHeight())/2);
                newActor.addActor(overlay);
            }
            newActor.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if(((Button) actor).isChecked())
                    {
                        setSelected((Button) actor);
                    }
                }
            });
            addActorObject(newActor);
        }
        for(Map.Entry<String, Button> slot :equipmentSlots.entrySet()) {
            if(slot.getValue().getChildren().size>=2)
                slot.getValue().removeActorAt(1,false);
            String equippedItem=Current.player().itemInSlot(slot.getKey());
            if(equippedItem == null || equippedItem.equals(""))
                continue;
            ItemData item = ItemData.getItem(equippedItem);
            if(item != null) {
                Image img = new Image(item.sprite());
                img.setX((slot.getValue().getWidth() - img.getWidth()) / 2);
                img.setY((slot.getValue().getHeight() - img.getHeight()) / 2);
                slot.getValue().addActor(img);
            }
        }
    }

    @Override
    public void enter() {
        updateInventory();
        //inventory.add().expand();
        super.enter();
    }

    public Button createInventorySlot() {

        ImageButton button=new ImageButton(Controls.getSkin(),"item_frame");
        return  button;
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            done();
        }
        if (keycode == Input.Keys.BUTTON_SELECT)
            performTouch(ui.findActor("return"));
        else if (keycode == Input.Keys.BUTTON_B)
            performTouch(ui.findActor("return"));
        else if (keycode == Input.Keys.BUTTON_A) {
            if (selectedActor instanceof ImageButton) {
                performTouch(equipButton);
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        selectCurrent();
                    }
                }, 0.25f);

            } else {
                performTouch(selectedActor);
            }
        } else if (keycode == Input.Keys.DPAD_RIGHT || keycode == Input.Keys.DPAD_DOWN)
            selectNextActor(false);
        else if (keycode == Input.Keys.DPAD_LEFT || keycode == Input.Keys.DPAD_UP)
            selectPreviousActor(false);
        return true;
    }
}
