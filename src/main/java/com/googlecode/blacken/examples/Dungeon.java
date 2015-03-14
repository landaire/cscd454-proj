﻿/* blacken - a library for Roguelike games
 * Copyright © 2012 Steven Black <yam655@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.googlecode.blacken.examples;

import com.cscd.game.event.EventDispatcherFactory;
import com.cscd.game.event.RecenterMapEvent;
import com.cscd.game.event.UpdateMessageEvent;
import com.cscd.game.factory.ConfigFactory;
import com.cscd.game.goals.DungeonGoals;
import com.cscd.game.model.characters.bad.Ogre;
import com.cscd.game.model.characters.good.*;
import com.cscd.game.model.classes.A_Class;
import com.cscd.game.ui.Color;
import com.cscd.game.ui.builder.CharacterBuilder;
import com.cscd.game.ui.character.*;
import com.googlecode.blacken.bsp.BSPTree;
import com.googlecode.blacken.colors.ColorNames;
import com.googlecode.blacken.colors.ColorPalette;
import com.googlecode.blacken.core.Obligations;
import com.googlecode.blacken.core.Random;
import com.googlecode.blacken.dungeon.Room;
import com.googlecode.blacken.dungeon.SimpleDigger;
import com.googlecode.blacken.examples.Dungeon;
import com.googlecode.blacken.extras.PerlinNoise;
import com.googlecode.blacken.grid.Grid;
import com.googlecode.blacken.grid.Point;
import com.googlecode.blacken.grid.Positionable;
import com.googlecode.blacken.swing.SwingTerminal;
import com.googlecode.blacken.terminal.*;

import java.lang.reflect.Array;
import java.util.*;

import jdk.nashorn.internal.runtime.regexp.joni.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A super-simple game
 * 
 * @author Steven Black
 */
public class Dungeon implements Observer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dungeon.class);
    /**
     * TerminalInterface used by the example
     */
    protected CursesLikeAPI term;
    /**
     * Whether to quit the loop or not
     */
    protected boolean quit;
    private Party player;
    private Grid<Integer> grid;
    private Random rand;
    private final static Positionable MAP_START = new Point(1, 0);
    private final static Positionable MAP_END = new Point(-1, 0);
    private Positionable upperLeft = new Point(0, 0);
    private Integer underPlayer = -1;
    private boolean dirtyMsg = false;
    private boolean dirtyStatus = false;
    private String message;
    private float noisePlane;
    private Set<Integer> passable;
    private List<Map<Integer, Representation>> representations = new ArrayList<>();
    private int represent = 0;
    private boolean splashShown = false;
    private boolean splashShown2 = false;
    private String helpMessage =
"Dungeon Example Commands\n" +
"============================================================================\n" +
"Ctrl+L : recenter and redisplay the screen\n" +
"j, Down : move down                  | k, Up : move up\n" +
"h, Left : move left                  | l (ell), Right: move right\n" +
"\n" +
"Space : next representation set      | Backspace : previous representations\n" +
"\n" +
"Q, q, Escape : quit\n" +
"\n" +
"L : show my license                  | N : show legal notices\n" +
"\n" +
"? : this help screen\n";

    public void addRepresentations() {

        // Initialize the room walls
        Set<Integer> roomWalls = new HashSet<>();
        // roomWalls.add(ConfigFactory.get("room:wall"));
        roomWalls.add(ConfigFactory.get("room:wall:top"));
        roomWalls.add(ConfigFactory.get("room:wall:left"));
        roomWalls.add(ConfigFactory.get("room:wall:bottom"));
        roomWalls.add(ConfigFactory.get("room:wall:right"));
        roomWalls.add(ConfigFactory.get("room:wall:top-left"));
        roomWalls.add(ConfigFactory.get("room:wall:top-right"));
        roomWalls.add(ConfigFactory.get("room:wall:bottom-left"));
        roomWalls.add(ConfigFactory.get("room:wall:bottom-right"));

        // default
        Representation e;
        Map<Integer, Representation> r;

        r = new HashMap<>();
        representations.add(r);

        e = new Representation();
        e.add(ConfigFactory.get("player"), Color.Yellow.value);
        r.put(ConfigFactory.get("player"), e);

        // Here, each player representation is added
        player = new Party(new PositionableObject[]{
                new PositionableObject(e),
                new PositionableObject(e),
                new PositionableObject(e),
                new PositionableObject(e),
                new PositionableObject(e),
        });

        e = new Representation();
        e.add(ConfigFactory.get("room:door"), 58, 130, 94, 94, 94, 94, 94, 94, 94, 94);
        r.put(ConfigFactory.get("room:door"), e);

        e = new Representation();
        e.add(ConfigFactory.get("floor"), 0xee, 10);
        r.put(ConfigFactory.get("floor"), e);

        e = new Representation();
        e.add(ConfigFactory.get("hall:floor"), 0xee, 10);
        r.put(ConfigFactory.get("hall:floor"), e);

        e = new Representation();
        e.add(ConfigFactory.get("diggable"), 0x58, 14);
        r.put(ConfigFactory.get("diggable"), e);

        e = new Representation();
        e.add(ConfigFactory.get("hall:wall"), 0x58, 14);
        r.put(ConfigFactory.get("hall:wall"), e);

        for (Integer roomWall : roomWalls) {
            e = new Representation();
            e.add(roomWall, 0x58, 14);
            r.put(roomWall, e);
        }

        e = new Representation();
        e.add(ConfigFactory.get("water"), 17, 11);
        e.add(ConfigFactory.get("mountains"), 236, 20);
        e.add(ConfigFactory.get("water"), 17, 11);
        e.add(ConfigFactory.get("water"), 17, 11);
        e.add(ConfigFactory.get("water"), 17, 11);
        e.add(ConfigFactory.get("mountains"), 236, 20);
        r.put(ConfigFactory.get("void"), e);

        for (char goal = '0'; goal <= '9'; goal++) {
            Integer g = new Integer(goal);
            e = new Representation();
            e.add(g, 0x4 + g - '0');
            r.put(g, e);
        }

        // nethack

        r = new HashMap<>();
        representations.add(r);
        e = new Representation();
        e.add("@".codePointAt(0), 7);
        r.put(ConfigFactory.get("player"), e);

        e = new Representation();
        e.add("+".codePointAt(0), 7);
        r.put(ConfigFactory.get("room:door"), e);

        e = new Representation();
        e.add(".".codePointAt(0), 7);
        r.put(ConfigFactory.get("floor"), e);

        e = new Representation();
        e.add("#".codePointAt(0), 7);
        r.put(ConfigFactory.get("hall:floor"), e);

        e = new Representation();
        e.add(" ".codePointAt(0), 0);
        r.put(ConfigFactory.get("diggable"), e);

        e = new Representation();
        e.add(" ".codePointAt(0), 0);
        r.put(ConfigFactory.get("hall:wall"), e);

        e = new Representation();
        e.add("-".codePointAt(0), 7);
        r.put(ConfigFactory.get("room:wall:top"), e);
        r.put(ConfigFactory.get("room:wall:bottom"), e);
        r.put(ConfigFactory.get("room:wall:top-left"), e);
        r.put(ConfigFactory.get("room:wall:top-right"), e);
        r.put(ConfigFactory.get("room:wall:bottom-left"), e);
        r.put(ConfigFactory.get("room:wall:bottom-right"), e);

        e = new Representation();
        e.add("|".codePointAt(0), 7);
        r.put(ConfigFactory.get("room:wall:left"), e);
        r.put(ConfigFactory.get("room:wall:right"), e);

        for (Integer roomWall : roomWalls) {
            if (!r.containsKey(roomWall)) {
                e = new Representation();
                e.add(roomWall, 0x58, 14);
                r.put(roomWall, e);
            }
        }

        e = new Representation();
        e.add(" ".codePointAt(0), 0);
        r.put(ConfigFactory.get("void"), e);

        for (char goal = '0'; goal <= '9'; goal++) {
            Integer g = new Integer(goal);
            e = new Representation();
            e.add(g, 0x4 + g - '0');
            r.put(g, e);
        }

        // moria

        r = new HashMap<>();
        representations.add(r);

        e = new Representation();
        e.add((int) '@', Color.Yellow.value);
        r.put(ConfigFactory.get("player"), e);

        e = new Representation();
        e.add((int) '+', 58, 130, 94, 94, 94, 94, 94, 94, 94, 94);
        e.add((int) '+', 58, 130, 94, 94, 94, 94, 94, 94, 94, 94);
        e.add((int) '\'', 58, 130, 94, 94, 94, 94, 94, 94, 94, 94);
        r.put(ConfigFactory.get("room:door"), e);

        e = new Representation();
        e.add((int) '.', 0xee, 10);
        r.put(ConfigFactory.get("floor"), e);

        r.put(ConfigFactory.get("hall:floor"), e);

        e = new Representation();
        e.add(BlackenCodePoints.CODEPOINT_MEDIUM_SHADE, 0x58, 14);
        e.add(BlackenCodePoints.CODEPOINT_LIGHT_SHADE, 0x58, 14);
        e.add(BlackenCodePoints.CODEPOINT_MEDIUM_SHADE, 0x58, 14);
        e.add(BlackenCodePoints.CODEPOINT_MEDIUM_SHADE, 0x58, 14);
        r.put(ConfigFactory.get("diggable"), e);

        e = r.get(ConfigFactory.get("diggable"));
        r.put(ConfigFactory.get("hall:wall"), e);

        for (Integer roomWall : roomWalls) {
            e = new Representation();
            e.add(BlackenCodePoints.CODEPOINT_MEDIUM_SHADE, 0x58, 14);
            r.put(roomWall, e);
        }

        e = new Representation();
        e.add(" ".codePointAt(0), 0);
        r.put(ConfigFactory.get("void"), e);

        for (char goal = '0'; goal <= '9'; goal++) {
            Integer g = new Integer(goal);
            e = new Representation();
            e.add(g, 0x4 + g - '0');
            r.put(g, e);
        }
    }

    /**
     * Create a new instance
     */
    public Dungeon() {
        rand = new Random();
        noisePlane = rand.nextFloat();

        grid = new Grid<>(ConfigFactory.get("diggable"), 100, 100);
        passable = new HashSet<>();
        passable.add(ConfigFactory.get("floor"));
        passable.add(ConfigFactory.get("hall:floor"));
        passable.add(ConfigFactory.get("room:door"));
    }

    public Grid<Integer> getGrid() {
        return this.grid;
    }

    /**
     * Make a map
     */
    private void makeMap() {
        grid.clear();
        SimpleDigger simpleDigger = new SimpleDigger();
        BSPTree<Room> bsp = simpleDigger.setup(grid, ConfigFactory.getConfig());
        List<Room> rooms = new ArrayList(bsp.findContained(null));
        Collections.shuffle(rooms, rand);
        DungeonGoals.nextLocation = 0x31;
        int idx = 0;
        for (Integer c = 0x31; c < 0x3a; c++) {
            rooms.get(idx).assignToContainer(c);
            idx++;
            if (idx >= rooms.size()) {
                idx = 0;
                Collections.shuffle(rooms, rand);
            }
        }
        // simpleDigger.digRoomAvoidanceHalls(bsp, grid, config);
        simpleDigger.digHallFirst(bsp, grid, ConfigFactory.getConfig(), false);
        underPlayer = ConfigFactory.get("room:floor");
        Positionable pos = rooms.get(idx).placeThing(grid, underPlayer, ConfigFactory.get("player"));
        this.player.setPosition(pos);

        recenterMap();
    }

    private void showMap() {
        int ey = MAP_END.getY();
        int ex = MAP_END.getX();
        if (ey <= 0) {
            ey += term.getHeight();
        }
        if (ex <= 0) {
            ex += term.getWidth();
        }
        Map<Integer, Representation> currentRep = this.representations.get(this.represent);
        for (int y = MAP_START.getY(); y < ey; y++) {
            for (int x = MAP_START.getX(); x < ex; x++) {
                int y1 = y + upperLeft.getY() - MAP_START.getY();
                int x1 = x + upperLeft.getX() - MAP_START.getX();
                int what = ConfigFactory.get("void");
                if (y1 >= 0 && x1 >= 0 && y1 < grid.getHeight() && x1 < grid.getWidth()) {
                    what = grid.get(y1, x1);
                }
                Representation how = currentRep.get(what);
                if (how == null) {
                    LOGGER.error("Failed to find entry for {}", BlackenKeys.toString(what));
                }
                double noise = PerlinNoise.noise(x1, y1, noisePlane);
                int as = how.getCodePoint(noise);
                int fclr = how.getColor(noise);
                int bclr = 0;
                EnumSet<CellWalls> walls = EnumSet.noneOf(CellWalls.class);
                if (what >= '0' && what <= '9') {
                    if (what > DungeonGoals.nextLocation) {
                        walls = CellWalls.BOX;
                    }
                }
                term.set(y, x, BlackenCodePoints.asString(as),
                         fclr, bclr, EnumSet.noneOf(TerminalStyle.class), walls);
            }
        }
    }
    
    /**
     * The application loop.
     * @return the quit status
     */
    public boolean loop() {
        makeMap();
        term.disableEventNotices();
        int ch = BlackenKeys.NO_KEY;
        int mod;
        updateStatus();
        player.moveBy(0,0);
        this.message = "Welcome to Dungeon!";
        term.move(-1, -1);
        while (!quit) {
            if (dirtyStatus) {
                updateStatus();
            }
            updateMessage(false);
            showMap();
            term.setCursorLocation(player.getY() - upperLeft.getY() + MAP_START.getY(),
                                   player.getX() - upperLeft.getX() + MAP_START.getX());
            this.term.getPalette().rotate(0xee, 10, +1);
            // term.refresh();
            mod = BlackenKeys.NO_KEY;
            ch = term.getch();
            if (ch == BlackenKeys.RESIZE_EVENT) {
                this.refreshScreen();
                continue;
            } else if (BlackenKeys.isModifier(ch)) {
                mod = ch;
                ch = term.getch();
            }
            // LOGGER.debug("Processing key: {}", ch);
            if (ch != BlackenKeys.NO_KEY) {
                this.message = null;
                doAction(mod, ch);
            }
        }
        return this.quit;
    }

    public boolean playerCanAccessPosition(Integer there) {
        return there.equals(ConfigFactory.get("player")) || passable.contains(there) || there == DungeonGoals.nextLocation;
    }

    private void updateMessage(boolean press) {
        if (this.message != null && !dirtyMsg) {
            dirtyMsg = true;
        }
        if (dirtyMsg) {
            for (int x = 0; x < term.getWidth(); x++) {
                term.mvaddch(0, x, ' ');
            }
            if (message == null) {
                dirtyMsg = false;
            } else {
                term.mvputs(0, 0, message);
            }
            if (press) {
                message = null;
            }
        }
    }

    /**
     * Update the status.
     */
    private void updateStatus() {
        term.setCurForeground(7);
        dirtyStatus = false;
        for (int x = 0; x < term.getWidth()-1; x++) {
            term.mvaddch(term.getHeight(), x, ' ');
        }

        Integer nextLocation = DungeonGoals.nextLocation;
        if (nextLocation <= '9') {
            term.mvputs(term.getHeight(), 0, "Get the ");
            term.setCurForeground((nextLocation - '0') + 0x4);
            term.addch(nextLocation);
            term.setCurForeground(7);
            if (nextLocation == '9') {
                term.puts(" to win.");
            }
        } else {
            term.mvputs(term.getHeight(), 0, "You won!");
        }
        String msg = "Q to quit.";
        term.mvputs(term.getHeight(), term.getWidth()-msg.length()-1, msg);
    }

    private void refreshScreen() {
        term.clear();
        updateStatus();
        updateMessage(false);
        this.showMap();
    }
    
    private boolean doAction(int modifier, int ch) {
        if (BlackenModifier.MODIFIER_KEY_CTRL.hasFlag(modifier)) {
            switch (ch) {
            case 'l':
            case 'L':
                this.recenterMap();
                refreshScreen();
                break;
            }
            return false;
        } else {
            switch (ch) {
            case 'j':
            case BlackenKeys.KEY_DOWN:
            case BlackenKeys.KEY_NP_2:
            case BlackenKeys.KEY_KP_DOWN:
                player.moveBy(+1,  0);
                break;
            case 'k':
            case BlackenKeys.KEY_UP:
            case BlackenKeys.KEY_NP_8:
            case BlackenKeys.KEY_KP_UP:
                player.moveBy(-1,  0);
                break;
            case 'h':
            case BlackenKeys.KEY_LEFT:
            case BlackenKeys.KEY_NP_4:
            case BlackenKeys.KEY_KP_LEFT:
                player.moveBy(0,  -1);
                break;
            case 'l':
            case BlackenKeys.KEY_RIGHT:
            case BlackenKeys.KEY_NP_6:
            case BlackenKeys.KEY_KP_RIGHT:
                player.moveBy(0,  +1);
                break;
            case ' ':
                this.represent ++;
                if (this.represent >= this.representations.size()) {
                    this.represent = 0;
                }
                break;
            case BlackenKeys.KEY_BACKSPACE:
                this.represent --;
                if (this.represent < 0) {
                    this.represent = this.representations.size() -1;
                }
                break;
            case 'q':
            case 'Q':
            case BlackenKeys.KEY_ESCAPE:
                this.quit = true;
                return false;
            case 'L':
                showMyLicense();
                refreshScreen();
                break;
            case 'N':
                showLegalNotices();
                refreshScreen();
                break;
            case 'F':
                showFontLicense();
                refreshScreen();
                break;
            case '?':
                showHelp();
                refreshScreen();
                break;
            default:
                return false;
            }
        }
        return true;
    }

    private void recenterMap() {
        upperLeft.setY(player.getY() - (term.getHeight()-2)/2);
        upperLeft.setX(player.getX() - (term.getWidth()-2)/2);
    }
    
    
    /**
     * Initialize the example
     * 
     * @param term alternate TerminalInterface to use
     * @param palette alternate ColorPalette to use
     */
    public void init(TerminalInterface term, ColorPalette palette) {
        if (term == null) {
            term = new SwingTerminal();
            term.init("Dungeon", 25, 80);
        }
        this.term = new CursesLikeAPI(term);
        if (palette == null) {
            palette = new ColorPalette();
            palette.addAll(ColorNames.XTERM_256_COLORS, false);
        } 
        this.term.setPalette(palette);

        EventDispatcherFactory.get().addObserver(this);
        addRepresentations();
    }


    public void splash() {
        if (splashShown) {
            return;
        }
        splashShown = true;

        boolean ready = false;
        term.disableEventNotices();
        while (!ready) {
            term.clear();
            term.setCurBackground(0);
            term.setCurForeground(7);
            centerOnLine(0, "Dungeon");
            centerOnLine(1, "A very fun dungeon game");
            centerOnLine(3, "Originally created by Steven Black (Copyright (C) 2010-2012)");
            centerOnLine(5, "Modified for CSCD 349 by Lander Brandt, Tony Moua, Sean Burright");
            centerOnLine(6, "Released under the Apache 2.0 License.");
            term.mvputs(8, 0, "HOW TO PLAY");
            term.mvputs(9, 0, "-----------");
            term.mvputs(10,0, "A representation of a map is shown.  You and your party are the");
            term.mvputs(11,0, "at sign (@).  The object is to run around collecting the numbers");
            term.mvputs(12,0, "in order.  The numbers have walls around them that only open up");
            term.mvputs(13,0, "if you've collected the previous number.");
            term.mvputs(15,0, "Use the arrow keys to move around.");
            term.mvputs(17,0, "You will randomly encounter opponents in halls");
            int last = term.getHeight() - 1;
            term.mvputs(last-1, 0, "Press '?' for Help.");
            alignRight(last-0, "Press any other key to continue.");
            int key = BlackenKeys.NO_KEY;
            while(key == BlackenKeys.NO_KEY) {
                // This works around an issue with the AWT putting focus someplace weird
                // if the window is not in focus when it is shown. It only happens on
                // startup, so a splash screen is the perfect place to fix it.
                // A normal game might want an animation at such a spot.
                key = term.getch(200);
            }
            // int modifier = BlackenKeys.NO_KEY;
            if (BlackenKeys.isModifier(key)) {
                // modifier = key;
                key = term.getch(); // should be immediate
            }
            switch(key) {
                case BlackenKeys.NO_KEY:
                case BlackenKeys.RESIZE_EVENT:
                    // should be safe
                    break;
                case 'l':
                case 'L':
                    showMyLicense();
                    break;
                case 'n':
                case 'N':
                    showLegalNotices();
                    break;
                case 'f':
                case 'F':
                    showFontLicense();
                    break;
                case '?':
                    showHelp();
                    break;
                default:
                    ready = true;
                    break;
            }
        }
    }

    /**
     * Quit the application.
     * 
     * <p>This calls quit on the underlying TerminalInterface.</p>
     */
    public void quit() {
        term.quit();
    }

    private void centerOnLine(int y, String string) {
        int offset = term.getWidth() / 2 - string.length() / 2;
        term.mvputs(y, offset, string);
    }

    private void alignRight(int y, String string) {
        int offset = term.getWidth() - string.length();
        if (term.getHeight() -1 == y) {
            offset--;
        }
        term.mvputs(y, offset, string);
    }

    private void showLegalNotices() {
        // show Notices file
        // This is the only one that needs to be shown for normal games.
        ViewerHelper vh;
        vh = new ViewerHelper(term, "Legal Notices", Obligations.getBlackenNotice());
        vh.setColor(7, 0);
        vh.run();
    }

    private void showFontLicense() {
        // show the font license
        ViewerHelper vh;
        new ViewerHelper(term,
                Obligations.getFontName() + " Font License",
                Obligations.getFontLicense()).run();
    }

    private void showHelp() {
        ViewerHelper vh;
        vh = new ViewerHelper(term, "Help", helpMessage);
        vh.setColor(7, 0);
        vh.run();
    }

    private void showMyLicense() {
        // show Apache 2.0 License
        ViewerHelper vh;
        vh = new ViewerHelper(term, "License", Obligations.getBlackenLicense());
        vh.setColor(7, 0);
        vh.run();
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof UpdateMessageEvent) {
            UpdateMessageEvent event = (UpdateMessageEvent)arg;
            this.message = event.message();

            this.dirtyStatus = event.dirtyStatus();
            this.updateMessage(event.updateMessage());
        } else if (arg instanceof RecenterMapEvent) {
            checkForRecenter(((RecenterMapEvent)arg).positionable());
        }
    }

    private void checkForRecenter(Positionable player) {
        grid.set(player.getY(), player.getX(), 0x40);
        int playerScreenY = player.getY() - upperLeft.getY() + MAP_START.getY();
        int playerScreenX = player.getX() - upperLeft.getX() + MAP_START.getX();
        int ScreenY2 = (MAP_END.getY() <= 0
                ? term.getHeight() -1 + MAP_END.getY() : MAP_END.getY());
        int ScreenX2 = (MAP_END.getX() <= 0
                ? term.getWidth() -1 + MAP_END.getX() : MAP_END.getX());
        if (playerScreenY >= ScreenY2 || playerScreenX >= ScreenX2 ||
                playerScreenY <= MAP_START.getY() ||
                playerScreenX <= MAP_START.getX()) {
            recenterMap();
        }
    }
    
    public void chooseCharacter()
    {
    	if (splashShown2) {
            return;
        }
        splashShown2 = true;
        int characterCount = 0;
        boolean ready = false;
        term.disableEventNotices();

        ArrayList<A_Class> chosenCharacters = new ArrayList<>(6);
        String chosenCharacterNames = "";
        while (!ready) {
            term.clear();
            term.setCurBackground(0);
            term.setCurForeground(7);
            centerOnLine(0, "Choose ");

            term.mvputs(3, 0, "1. Beast");
            term.mvputs(5, 0, "2. Hospital");
            term.mvputs(7,0, "3. Hunter");
            term.mvputs(9,0, "4. Mage");
            term.mvputs(11,0, "5. Ninja");
            term.mvputs(13,0, "6. Paladin");
            term.mvputs(15,0, "7. Warlock");
            term.mvputs(17,0, "Chosen characters: " + chosenCharacterNames);
            int last = term.getHeight() - 1;
            term.mvputs(last-1, 0, "Press '?' for Help.");
            if(characterCount >= 2)
            {
            	alignRight(last-0, "Press any other key to continue.");
            	
            	//need a way to disable Enter key until they have 2 or more in the party
            }
            //alignRight(last-0, "Press any other key to continue.");
            int key = BlackenKeys.NO_KEY;
            while(key == BlackenKeys.NO_KEY) {
                // This works around an issue with the AWT putting focus someplace weird
                // if the window is not in focus when it is shown. It only happens on
                // startup, so a splash screen is the perfect place to fix it.
                // A normal game might want an animation at such a spot.
                key = term.getch(200);
            }
            // int modifier = BlackenKeys.NO_KEY;
            if (BlackenKeys.isModifier(key)) {
                // modifier = key;
                key = term.getch(); // should be immediate
            }
            switch(key) {
                case BlackenKeys.NO_KEY:
                case BlackenKeys.RESIZE_EVENT:
                    // should be safe
                    break;
                case '1':
                    chosenCharacters.add(new Beast());
                    chosenCharacterNames += "Beast, ";
                    characterCount = characterCount+1;
                    break;
                case '2':
                    chosenCharacters.add(new Hospital());
                    chosenCharacterNames += "Hospital, ";
                    characterCount = characterCount+1;
                	break;
                case '3':
                	chosenCharacters.add(new Hunter());
                    chosenCharacterNames += "Hunter, ";
                    characterCount = characterCount+1;
                    break;
                case '4':
                	chosenCharacters.add(new Mage());
                    chosenCharacterNames += "Mage, ";
                    characterCount = characterCount+1;
                    break;
                case '5':
                	chosenCharacters.add(new Ninja());
                    chosenCharacterNames += "Ninja, ";
                    characterCount = characterCount+1;
                    break;
                case '6':
                	chosenCharacters.add(new Paladin());
                    chosenCharacterNames += "Paladin, ";
                    characterCount = characterCount+1;
                    break;
                case '7':
                	chosenCharacters.add(new Warlock());
                    chosenCharacterNames += "Warlock, ";
                    characterCount = characterCount+1;
                    break;
                case '?':
                    showHelp();
                    break;
                default:
                    ready = true;
                    break;
            }
        }
    }//end choose
}
