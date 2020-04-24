import java.util.*;
import java.io.*;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;  

// this class computes various statistics
// this class can be modified in order to collect the desired statistics

class Stats {

    // ---------------------------- REPORT -----------------------------
    PrintWriter out = null;
    long startTime;
    final int INF = Integer.MAX_VALUE;

    void print(String s) {
        out.print(s);
        if (CaveViewer.active) {
            CaveViewer.caveViewer.reportBuffer.append(s);
        }
    }
    void println(String s) {
        print(s + "\n");
    }

    // this function gets called once at the start of the process
    public Stats(String args[]) {
        if (!CaveGen.showStats) return;
        if (CaveGen.findGoodLayouts) {
            Parser.readConfigFiles();
        }
        try {
            startTime = System.currentTimeMillis();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
            LocalDateTime now = LocalDateTime.now();  
            String dateString = dtf.format(now);
            String output = CaveGen.p251 ? "output251" : "output";
            new File(output+"/").mkdir();
            new File(output + "/!reports/").mkdir();
            String outputFileName = output + "/!reports/report-" + dateString + ".txt";
            out = new PrintWriter(new BufferedWriter(new FileWriter(outputFileName)));
            print("CaveGen ");
            for (String s: args) {
                print(s + " ");
            }
            println("\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    int caveGenCount = 0;
    int numPurpleFlowers[] = new int[6];
    int missingTreasureCount = 0;
    SortedList<Integer> allScores = new SortedList<Integer>(Comparator.naturalOrder());

    // this function gets called once for every sublevel g that generates
    void analyze(CaveGen g) {
        caveGenCount += 1; 

        // count the number of purple flowers
        int num = 0;
        for (Teki t: g.placedTekis) {
            if (t.tekiName.equalsIgnoreCase("blackpom"))
                num += 1;
        }
        if (num > 5) num = 5;
        numPurpleFlowers[num] += 1;

        // report about missing treasures
        // print the seed everytime we see a missing treasure
        int minTreasure = 0, actualTreasure = 0;
        for (Item t: g.spawnItem) { minTreasure += t.min; }
        for (Teki t: g.spawnTekiConsolidated) { if (t.itemInside != null) minTreasure += t.min; }
        actualTreasure += g.placedItems.size();
        for (Teki t: g.placedTekis) {
            if (t.itemInside != null)
                actualTreasure += 1;
        }
        int expectedMissingTreasures = 0;
        if ("CH29 1".equals(g.specialCaveInfoName + " " + g.sublevel))
            expectedMissingTreasures = 1; // This level is always missing a treasure
        boolean missingUnexpectedTreasure = actualTreasure + expectedMissingTreasures < minTreasure;
        if (missingUnexpectedTreasure) {
            println("Missing treasure: " + g.specialCaveInfoName + " " + g.sublevel + " " + Drawer.seedToString(g.initialSeed));
            missingTreasureCount += 1;
        }

        // Good layout finder (story mode)
        if (CaveGen.findGoodLayouts && !CaveGen.challengeMode && !missingUnexpectedTreasure) {
            boolean giveWorstLayoutsInstead = CaveGen.findGoodLayoutsRatio < 0;

            ArrayList<Teki> placedTekisWithItems = new ArrayList<Teki>();
            for (Teki t: g.placedTekis) {
                if (t.itemInside != null)
                placedTekisWithItems.add(t);
            }

            String ignoreItems = "g_futa_kyodo,flower_blue,tape_blue,kinoko_doku,flower_red,futa_a_silver,cookie_m_l,chocolate";
            String findTekis = ""; //"whitepom,blackpom";

            // Compute the waypoints on the shortest paths
            ArrayList<WayPoint> wpOnShortPath = new ArrayList<WayPoint>();
            for (Item t: g.placedItems) { // Treasures
                if (ignoreItems.contains(t.itemName.toLowerCase())) continue;
                WayPoint wp = g.closestWayPoint(t.spawnPoint);
                while (!wp.isStart) {
                    if (!wpOnShortPath.contains(wp)) wpOnShortPath.add(wp);
                    wp = wp.backWp;
                }
            }
            for (Teki t: placedTekisWithItems) { // Treasures inside enemies
                if (ignoreItems.contains(t.itemInside.toLowerCase())) continue;
                WayPoint wp = g.closestWayPoint(t.spawnPoint);
                while (!wp.isStart) {
                    if (!wpOnShortPath.contains(wp)) wpOnShortPath.add(wp);
                    wp = wp.backWp;
                }
            }
            for (Teki t: g.placedTekis) { // Other tekis
                if (findTekis.contains(t.tekiName.toLowerCase())) {
                    WayPoint wp = g.closestWayPoint(t.spawnPoint);
                    while (!wp.isStart) {
                        if (!wpOnShortPath.contains(wp)) wpOnShortPath.add(wp);
                        wp = wp.backWp;
                    }
                }
            }
            /*if (g.placedHole != null) {
                WayPoint wp = g.closestWayPoint(g.placedHole);
                while (!wp.isStart) {
                    if (!wpOnShortPath.contains(wp)) wpOnShortPath.add(wp);
                    wp = wp.backWp;
                }
            }
            if (g.placedGeyser != null) {
                WayPoint wp = g.closestWayPoint(g.placedGeyser);
                while (!wp.isStart) {
                    if (!wpOnShortPath.contains(wp)) wpOnShortPath.add(wp);
                    wp = wp.backWp;
                }
            }*/

            // add up distance penalty for score
            int score = 0;
            for (WayPoint wp: wpOnShortPath) {
                score += wp.distToStart - wp.backWp.distToStart;
            }    
            // add up enemy penalties for score
            for (Teki t: g.placedTekis) {
                WayPoint wp = g.closestWayPoint(t.spawnPoint);
                if (wpOnShortPath.contains(wp)) {
                    score += Parser.tekiDifficulty.get(t.tekiName.toLowerCase());
                }
            }
            // add up gate penalties for score
            for (Gate t: g.placedGates) {
                WayPoint wp = g.closestWayPoint(t.spawnPoint);
                if (g.placedHole != null && g.placedHole.mapUnit.type == 0 && g.placedHole.mapUnit.doors.get(0).spawnPoint == t.spawnPoint)
                    score += t.life / 3; // covers hole
                // if (g.placedGeyser != null && g.placedGeyser.mapUnit.type == 0 && g.placedGeyser.mapUnit.doors.get(0).spawnPoint == t.spawnPoint)
                //    score += t.life / 3; // covers geyser
                if (wpOnShortPath.contains(wp))
                    score += t.life / 3; // covers path back to ship
            }

            if (giveWorstLayoutsInstead) score *= -1;

            // keep a sorted list of the scores
            allScores.add(score);

            // only print good ones
            if (CaveGen.indexBeingGenerated > CaveGen.numToGenerate/10 && 
                score <= allScores.get((int)(allScores.size()*Math.abs(CaveGen.findGoodLayoutsRatio))) 
                || score == allScores.get(0) && CaveGen.indexBeingGenerated > CaveGen.numToGenerate/40) {
                CaveGen.images = true;
                println("GoodLayoutScore: " + Drawer.seedToString(g.initialSeed) + " -> " + score);
            }
            else {
                CaveGen.images = false;
            }

        }

        // good layout finder (challenge mode)
        if (CaveGen.findGoodLayouts && CaveGen.challengeMode) {
            boolean giveWorstLayoutsInstead = CaveGen.findGoodLayoutsRatio < 0;

            // compute the number of pokos availible
            int pokosAvailible = 0;
            for (Teki t: g.placedTekis) {
                String name = t.tekiName.toLowerCase();
                if (plantNames.contains("," + name + ",")) continue;
                if (hazardNames.contains("," + name + ",")) continue;
                if (name.equalsIgnoreCase("egg"))
                    pokosAvailible += 10; // mitites
                else if (!noCarcassNames.contains("," + name + ",") && !name.contains("pom"))
                    pokosAvailible += Parser.pokos.get(t.tekiName.toLowerCase());
                if (t.itemInside != null)
                    pokosAvailible += Parser.pokos.get(t.itemInside.toLowerCase());
            }
            for (Item t: g.placedItems)
                pokosAvailible += Parser.pokos.get(t.itemName.toLowerCase());

            // compute the number of pikmin*seconds required to complete the level
            float pikminSeconds = 0;
            for (Teki t: g.placedTekis) {
                if (plantNames.contains("," + t.tekiName.toLowerCase() + ",")) continue;
                if (hazardNames.contains("," + t.tekiName.toLowerCase() + ",")) continue;
                pikminSeconds += workFunction(g, t.tekiName, t.spawnPoint);
                if (t.itemInside != null)
                    pikminSeconds += workFunction(g, t.itemInside, t.spawnPoint);
            }
            for (Item t: g.placedItems) {
                pikminSeconds += workFunction(g, t.itemName, t.spawnPoint);
            }
            pikminSeconds += workFunction(g, "hole", g.placedHole);
            pikminSeconds += workFunction(g, "geyser", g.placedGeyser);
            // gates??
            // hazards??
            
            int score = -pokosAvailible * 1000 + (int)(pikminSeconds/2);
            if (giveWorstLayoutsInstead) score *= -1;

            // keep a sorted list of the scores
            allScores.add(score);

            // only print good ones
            if (CaveGen.indexBeingGenerated > CaveGen.numToGenerate/10 && 
                score <= allScores.get((int)(allScores.size()*Math.abs(CaveGen.findGoodLayoutsRatio))) 
                || score == allScores.get(0) && CaveGen.indexBeingGenerated > CaveGen.numToGenerate/40) {
                CaveGen.images = true;
                println("GoodLayoutScore: " + Drawer.seedToString(g.initialSeed) + " -> " + score);
            }
            else {
                CaveGen.images = false;
            }
        }
    }

    static String plantNames = ",ooinu_s,ooinu_l,wakame_s,wakame_l,kareooinu_s,kareooinu_l,daiodored,"
            + "daiodogreen,clover,hikarikinoko,tanpopo,zenmai,nekojarashi,tukushi,magaret,watage,chiyogami,";     
    static String hazardNames = ",gashiba,hiba,elechiba,rock,";
    static String noCarcassNames = ",wealthy,fart,kogane,mar,hanachirashi,damagumo,bigfoot,bigtreasure,qurione,baby,bomb,egg,kurage,onikurage,bombotakara,blackman,tyre,";

    private float workFunction(CaveGen g, String name, SpawnPoint sp) {
        if (sp == null) return 0;
        name = name.toLowerCase();
        if (name.equals("hole"))
            return g.closestWayPoint(sp).distToStart / 170.0f * 4;
        if (name.equals("geyser"))
            return g.closestWayPoint(sp).distToStart / 170.0f * 20;
        if (name.equals("egg"))
            return 7 * 10 * g.closestWayPoint(sp).distToStart / 580.0f;
        if (name.contains("pom"))
            return g.closestWayPoint(sp).distToStart / 170.0f * 10;
        if (noCarcassNames.contains(","+name+",")) return 0;
        int minCarry = Parser.minCarry.get(name);
        int maxCarry = Parser.maxCarry.get(name);
        return 7 * minCarry * g.closestWayPoint(sp).distToStart
                    / (220.0f + 180.0f * (2 * minCarry - minCarry + 1) / maxCarry);
    }

    // this function gets called once at the end of the process
    void createReport() {

        // report about purple flowers
        println("\nPurple flower distribution: ");
        for (int i = 0; i < 6; i++) {
            println(i + (i==5?"+":"") + ": " + numPurpleFlowers[i]);
        }
        // report about missing treasures
        println("Missing treasure count: " + missingTreasureCount);

        println("\nGenerated " + caveGenCount + " sublevels.");
        println("Total run time: " + (System.currentTimeMillis()-startTime)/1000.0 + "s");

        out.close();
    }

    // ---------------------------- sHORT CIRCUIT -----------------------------

    // Check for a short circuit (returns true to short circuit)
    boolean checkForShortCircuit(CaveGen g) {
        int i = g.placedMapUnits.size() - 1;
        MapUnit m = g.placedMapUnits.get(i);
        if (i >= Parser.scUnitTypes.length) return false;
        if (Parser.scUnitTypes[i] != -1) {
            String targetName = g.spawnMapUnitsSorted.get(Parser.scUnitTypes[i]).name;
            if (!targetName.equals(m.name))
                return true;
        } 
        if (Parser.scRots[i] != -1) {
            if (m.rotation != Parser.scRots[i])
                return true;
        } 
        if (Parser.scUnitIdsFrom[i] != -1 
                && Parser.scDoorsFrom[i] != -1
                && Parser.scDoorsTo[i] != -1) {
            Door d = m.doors.get(Parser.scDoorsTo[i]);
            if (d.adjacentDoor == null || d.adjacentDoor.mapUnit == null)
                return true;
            MapUnit o = d.adjacentDoor.mapUnit;
            if (g.placedMapUnits.indexOf(o) != Parser.scUnitIdsFrom[i])
                return true;
            if (o.doors.indexOf(d.adjacentDoor) != Parser.scDoorsFrom[i])
                return true;
        }
        else {
            if (Parser.scDoorsTo[i] != -1) {
                Door d = m.doors.get(Parser.scDoorsTo[i]);
                if (d.adjacentDoor == null || d.adjacentDoor.mapUnit == null)
                    return true;
            }
            if (Parser.scUnitIdsFrom[i] != -1 
                    && Parser.scDoorsFrom[i] != -1) {
                boolean isGood = false;
                for (Door d: m.doors) {
                    if (d.adjacentDoor == null || d.adjacentDoor.mapUnit == null)
                        continue;
                    MapUnit o = d.adjacentDoor.mapUnit;
                    if (g.placedMapUnits.indexOf(o) != Parser.scUnitIdsFrom[i])
                        continue;
                    if (o.doors.indexOf(d.adjacentDoor) != Parser.scDoorsFrom[i])
                        continue;
                    isGood = true;
                }
                if (!isGood) return true;
            }
            else if (Parser.scUnitIdsFrom[i] != -1) {
                boolean isGood = false;
                for (Door d: m.doors) {
                    if (d.adjacentDoor == null || d.adjacentDoor.mapUnit == null)
                        continue;
                    MapUnit o = d.adjacentDoor.mapUnit;
                    if (g.placedMapUnits.indexOf(o) != Parser.scUnitIdsFrom[i])
                        continue;
                    isGood = true;
                }
                if (!isGood) return true;
            }
        }
        return false;
    }

    // ---------------------------- EXPECTATION TESTS -----------------------------

    // The purpose of these functions is to compare the output from the currect run
    // with the "expected_output.txt" file, and see if they are the same.
    // This is helpful to see if debugging has unexpectedly produced changes to the tool.
    ArrayList<String> expect = new ArrayList<String>();
    
    void setupExpectTests() {
        expect = new ArrayList<String>();

        CaveGen.caveInfoName = "both";
        CaveGen.numToGenerate = 100;
        CaveGen.firstGenSeed = 0;
        CaveGen.prints = false;
        CaveGen.images = false;
        CaveGen.folderSeed = false;
        CaveGen.folderCave = false;
        CaveGen.region = "us";
        CaveGen.fileSystem = "gc";
    }

    void outputSublevelForExpect(CaveGen g) {
        String s = CaveGen.specialCaveInfoName + " " + CaveGen.sublevel + " on seed " + Drawer.seedToString(g.initialSeed);
        System.out.println("Checking: " + s);
        expect.add("Generating " + s);
        for (MapUnit m: g.placedMapUnits) {
            expect.add("Placed: " + m.name + " " + m.rotation + " " + m.offsetX + "," + m.offsetZ);
        }
        for (Teki t: g.placedTekis) {
            expect.add("Spawned: " + t.tekiName + " " + t.type + (t.fallType == 0 ? "" : "f") + " " + t.posX + "," + t.posZ); 
        }
        for (Item t: g.placedItems) {
            expect.add("Spawned: " + t.itemName + " " + "2" + " " + t.posX + "," + t.posZ); 
        }
        for (Gate t: g.placedGates) {
            expect.add("Spawned: " + "gate" + t.life + " " + "5g" + " " + t.posX + "," + t.posZ); 
        }
    }

    void checkExpectation() {
        try {
            BufferedWriter wr = new BufferedWriter(new FileWriter("expected_output_this.txt"));
            for (String s: expect) wr.write(s + "\n");
            wr.close();

            Scanner sc = new Scanner(new FileReader("expected_output.txt"));
            for (int i = 0; i < expect.size(); i++) {
                if (sc.hasNextLine()) {
                    String s = sc.nextLine();
                    if (!s.equals(expect.get(i))) {
                        System.out.println("Expectation test failed on line " + (i+1));
                        System.out.println("Expected: " + s);
                        System.out.println("Got:      " + expect.get(i));
                        return;
                    }
                } else {
                    System.out.println("Expectation test failed (output too long");
                    return;
                }
            }
            if (sc.hasNext()) {
                System.out.println("Expectation test failed (output too short)");
                return;
            }
            
            sc.close();
            System.out.println("Expectation test passed!");
            println("Expectation test passed!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Expectation test failed");
            println("Expectation test failed");
        } 
    }

    // ---------------------------- MEMO (SUBLEVEL to .txt generation) -----------------------------

    // write the contents of the generated sublevel to a txt file for later recovery.
    void writeMemo(CaveGen g) {
        try {
            new File("output/!memo").mkdir();
            BufferedWriter wr = new BufferedWriter(new FileWriter("output/!memo/" 
                + g.specialCaveInfoName + "-" + g.sublevel + ".txt", true));
            
            wr.write(">" + Drawer.seedToString(g.initialSeed) + "\n");
            wr.write("M");
            for (MapUnit m: g.placedMapUnits) {
                wr.write("|" + m.spawnListIdx + "r" + m.rotation + "x" + m.offsetX + "z" + m.offsetZ);
            }
            wr.write("\n");
            if (g.placedStart != null) {
                wr.write("S|" + logSpawnPoint(g.placedStart) + "\n");
            }
            if (g.placedHole != null) {
                wr.write("H|" + logSpawnPoint(g.placedHole) + "\n");
            }
            if (g.placedGeyser != null) {
                wr.write("G|" + logSpawnPoint(g.placedGeyser) + "\n");
            }
            for (int type: new int[] {5,8,1,0,6}) {
                boolean placed = false;
                for (Teki t: g.placedTekis) {
                    if (t.type == type && t.spawnPoint.type != 9) {
                        if (!placed) {
                            wr.write(type + "");
                            placed = true;
                        }
                        wr.write("|" + t.spawnListIdx + logSpawnPoint(t.spawnPoint));
                    }
                }
                if (placed) wr.write("\n");
            }
            {
                boolean placed = false;
                for (Item t: g.placedItems) {
                        if (!placed) {
                            wr.write(2 + "");
                            placed = true;
                        }
                        wr.write("|" + t.spawnListIdx + logSpawnPoint(t.spawnPoint));
                }
                if (placed) wr.write("\n");
            }
            for (char type: new char[] {'c','f'}) {
                boolean placed = false;
                for (Teki t: g.placedTekis) {
                    if (t.spawnPoint.type == 9) {
                        boolean c = type == 'c' && (t.fallType == 0 || g.isPomGroup(t));
                        boolean f = type == 'f' && !(t.fallType == 0 || g.isPomGroup(t));
                        if (c || f) {
                            if (!placed) {
                                wr.write(type + "");
                                placed = true;
                            }
                            wr.write("|" + t.spawnListIdx + logSpawnPoint(t.spawnPoint));
                        }
                    }
                }
                if (placed) wr.write("\n");
            }
            {
                boolean placed = false;
                for (Gate t: g.placedGates) {
                        if (!placed) {
                            wr.write("g");
                            placed = true;
                        }
                        wr.write("|" + t.spawnListIdx + logSpawnPoint(t.spawnPoint));
                }
                if (placed) wr.write("\n");
            }
            wr.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Log failed");
        } 
    }

    String logSpawnPoint(SpawnPoint sp) {
        if (sp.door != null) 
            return "m" + sp.door.mapUnit.placedListIdx + "d" + sp.door.idx;
        else return "m" + sp.mapUnit.placedListIdx + "s" + sp.spawnListIdx;
    }

    void readMemo(CaveGen g) {
        String sublevel = g.specialCaveInfoName + "-" + g.sublevel;
        String seed = Drawer.seedToString(g.initialSeed);
        ArrayList<String> memo = readStoreFile(sublevel, seed);

        // first line is always map units
        g.sortAndRotateMapUnits();
        String[] ms = memo.get(0).split("[|]");
        for (int i = 1; i < ms.length; i++) {
            String[] m = ms[i].split("[rxz]");
            int type = Integer.parseInt(m[0]);
            int r = Integer.parseInt(m[1]);
            int x = Integer.parseInt(m[2]);
            int z = Integer.parseInt(m[3]);
            MapUnit mPlaced = g.spawnMapUnitsSortedAndRotated.get(type * 4 + r).copy();
            mPlaced.offsetX = x;
            mPlaced.offsetZ = z;
            mPlaced.placedListIdx = g.placedMapUnits.size();
            g.placedMapUnits.add(mPlaced);
            g.closeDoorCheck(mPlaced);
        }
        g.addSpawnPoints();
        //g.buildWayPointGraph();

        for (int j = 1; j < memo.size(); j++) {
            ms = memo.get(j).split("[|]");
            for (int i = 1; i < ms.length; i++) {
                String[] m = ms[i].split("[msd]");
                int type = m[0].length() == 0 ? -1 : Integer.parseInt(m[0]);
                int mm = Integer.parseInt(m[1]);
                int sd = Integer.parseInt(m[2]);
                MapUnit mu = g.placedMapUnits.get(mm);
                SpawnPoint spot = ms[i].contains("d") 
                    ? mu.doors.get(sd).spawnPoint
                    : mu.spawnPoints.get(sd);
                Teki spawn;
                switch (ms[0].charAt(0)) {
                case 'S':
                    g.placedStart = spot;
                    break;
                case 'H':
                    g.placedHole = spot;
                    break;
                case 'G':
                    g.placedGeyser = spot;
                    break;
                case '5':
                    spawn = g.spawnTeki5.get(type).spawn(mu, spot);
                    g.setSpawnTekiPos(spawn, spot, false);
                    g.placedTekis.add(spawn);
                    break;
                case '8':
                    spawn = g.spawnTeki8.get(type).spawn(mu, spot);
                    g.setSpawnTekiPos(spawn, spot, false);
                    g.placedTekis.add(spawn);
                    break;
                case '1':
                    spawn = g.spawnTeki1.get(type).spawn(mu, spot);
                    g.setSpawnTekiPos(spawn, spot, false);
                    g.placedTekis.add(spawn);
                    break;
                case '0':
                    spawn = g.spawnTeki0.get(type).spawn(mu, spot);
                    g.setSpawnTekiPos(spawn, spot, false);
                    float radius = spot.maxNum == 1 ? 0 : spot.radius * 0.67f;
                    float ang = (float)Math.PI * 2 * (((i % spot.maxNum) * 1.0f / spot.maxNum + spot.posX * 12.34567f + spot.posZ * 98.76543f) % 1);
                    spawn.posX += Math.sin(ang) * radius;
                    spawn.posZ += Math.cos(ang) * radius;
                    spawn.ang = ang;
                    g.placedTekis.add(spawn);
                    break;
                case '6':
                    spawn = g.spawnTeki6.get(type).spawn(mu, spot);
                    g.setSpawnTekiPos(spawn, spot, false);
                    g.placedTekis.add(spawn);
                    break;
                case 'c':
                    spawn = g.spawnCapTeki.get(type).spawn(mu, spot);
                    g.setSpawnTekiPos(spawn, spot, false);
                    g.placedTekis.add(spawn);
                    break;
                case 'f':
                    spawn = g.spawnCapFallingTeki.get(type).spawn(mu, spot);
                    g.setSpawnTekiPos(spawn, spot, false);
                    g.placedTekis.add(spawn);
                    break;
                case '2':
                    Item spawnI = g.spawnItem.get(type).spawn(mu, spot);
                    g.setSpawnItemPos(spawnI, spot);
                    g.placedItems.add(spawnI);
                    break;
                case 'g':
                    Gate spawnG = g.spawnGate.get(type).spawn(mu, spot);
                    g.setSpawnGatePos(spawnG, spot);
                    g.placedGates.add(spawnG);
                    break;
                }
            }
            
        }
        
    }


    String storeSublevel = "";
    ArrayList<String> storeLines;
    HashMap<String, Integer> storeSeeds;

    ArrayList<String> readStoreFile(String sublevel, String seed) {
        if (!sublevel.equals(storeSublevel)) {
            try {
                storeSublevel = sublevel;
                storeLines = new ArrayList<String>();
                storeSeeds = new HashMap<String,Integer>();
                Scanner sc = new Scanner(new FileReader("output/!memo/" + sublevel + ".txt"));
                while (sc.hasNextLine()) {
                    String s = sc.nextLine();
                    storeLines.add(s);
                    if (s.contains(">")) {
                        storeSeeds.put(s, storeLines.size()-1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("readStoreFile failed");
            }
        }

        ArrayList<String> ret = new ArrayList<String>();
        for (int i = storeSeeds.get(">" + seed) + 1; i < storeLines.size(); i++) {
            if (storeLines.get(i).contains(">"))
                break;
            ret.add(storeLines.get(i));
        }
        return ret;
    }
}
