DOUBLE_O<T>, BOOLEANO<T>， INT_O<T> 这些都有一个 O 后缀, O 可能是 object 的意思.
他们的共同点是可以从 T 里面获取一个 Double / Boolean / Int.

有一些 OE 后缀的版本, 是加上了 set 的功能, 可以 set 一个 Object T 的 double 值.

`GETTER_TRANS<F, T>` 可以看作是 `Map<F, T>`, 输入 F 可以查询到 T. 也有 E 后缀的版本. 多了 set 功能.
所以 E 后缀应该是 edit 的意思.

DOUBLE, INT, LONG, BOOLEAN 之类的都有一个无参数的 `get?` 方法. 可以看作就是普通的 int / long / double. 因为 `() => A` 和 `A` 是同构的.

## FilePutter, FileGetter
Putter 提供了 i, b, l, 之类的方法来写入 int, bool, long. 会把他们先写入到一个 byte buffer, 调用 save 的时候写磁盘. 
一般的用法是
```java
protected void save(FilePutter file) {
    file.i(playerHealth);           // Write int
    file.d(playerX);                // Write double
    file.bool(isAlive);            // Write boolean
    file.chars(playerName);         // Write string
    file.isE(inventoryIds);         // Write int array with length
}
```
Getter 需要用一致的顺序从 binary data 里面读回对应的字段
```java
protected void load(FileGetter file) throws IOException {
    int health = file.i();           // Read: 4 bytes (int)
    double x = file.d();             // Read: 8 bytes (double)
    boolean alive = file.bool();     // Read: 1 byte (boolean)
    String name = file.chars();      // Read: 4 bytes (length) + N*2 bytes (string)
    int[] ids = new int[/* need to know size or read length */];
    file.isE(ids);                    // Read: 4 bytes (length) + N*4 bytes (array)
}
```

有 mark 和 check 两个方法可以验证读到的数据是写入时的数据
```java
// In save()
file.mark("PlayerData");  // Writes hash code of "PlayerData"
// In load()
file.check("PlayerData"); // Verifies hash code matches
```

Savable 就是游戏里面需要保存到硬盘的数据, 需要实现 save 和 load 两个方法
```java
protected abstract void save(FilePutter file);
protected abstract void load(FileGetter file) throws IOException;
```

## GameResource
```java
public abstract static class GameResource extends Savable {
    protected abstract void update(float ds, Profiler prof)
}
```
所有可以保存, 并且可以 update 的东西, 包含 SETT, EVENTS, NOBLES, TOURISM 之类的.

SETT.SettResource 也是类似, 虽然没有继承关系, 但是 SettResource 基本上也是可以 save 和 update 的.
```java
public static abstract class SettResource {
		
    private final static LinkedList<SettResource> resources = new LinkedList<SettResource>();

    // ...
    
    protected void save(FilePutter file) {}
    
    protected void load(FileGetter file) throws IOException{}
    
    protected void clear(){}
    
    protected void generate(CapitolArea area){ }
    
    protected void update(float ds, Profiler profiler){}
    
    /**
     * Will be called once after the settlement has renderered
     * @param ds
     */
    protected void postRender(float ds) {}
    
    protected void afterTick() {}
    
    protected void init(boolean loaded) {}

}
```

## CORE_STATE
游戏划分成 Launcher, Memu, VIEW 几个阶段, 都实现了 CORE_STATE.
VIEW 是游戏主体部分.

## snake2d.Renderer
使用只需要提供 2d coordinate 和贴图之类的, 实现会把贴图渲染到对应位置. 还管理了 zoom 和 culling. 使用方只需要考虑位置. 还有其他的灯光之类的这里也处理了.

## VIEW
render 的主入口, 会分别调用各个子系统的 render 函数.
例如
```java
mouse.render(r, ds); // 渲染鼠标
inters.mouseMessage.render(r, ds); // tooltips
GAME.script().callback.render(r, ds); // MOD 可以拓展 render
if (!inters.manager.render(r, ds)) {
    return;
}
if (!current.uiManager.render(r, ds))
    return;
// 主要内容, current game state, SettView, WorldView, BattleView 之类的都在这里
current.render(r, ds, false);
```

## SettView
render 的时候会把游戏主页面内容的东西交给 SETT.render. 其他 UI 相关的东西在
```java
public final UIPanelTopSett ui;        // Top UI panel
public final SettUI misc;               // Misc UI elements
public final ISidePanels panels;        // Side panels
public final UIMinimap mini;            // Minimap
```

Tools 比较重要, 是游戏里面的建造和摆放相关的
```java
public final ToolManager tools = new ToolManager(uiManager, window);
```

## Interrupter
所有 UI 相关的 base class.
有一个 InterManager 管理多个 Interrupter. 他们是有层级关系的
```
InterManager (Stack)
├── Interrupter A (bottom)
├── Interrupter B
└── Interrupter C (top) ← Receives input first
```
每个 Interrupter 主要实现 show hide pin, mouseClick 之类的.

## Tool
```
Default Tool (ToolDefault)
  ├── Click entities → Interact
  ├── Click rooms → View room info
  ├── Hold MOD + Click → Place more of job/room
  └── Right-click → Cancel/Back
     
Placement Tool (ToolPlacer)
  ├── Show placement preview
  ├── Left-click → Place object
  ├── Right-click → Cancel placement
  └── Undo button → Undo last placement
```

## ToolManager
place 每次在 UI 页面上选择需要规划的工具时调用. 例如创建新房间, 复制房间, 复制区域.
复制区域分成两个 tool, First 是复制阶段, Second 是粘贴阶段.

## PLACABLE, PlacableFixed, PlacableMulti
PLACABLE 是 ToolPlacer 调用 place 需要传入参数的基类. 

PlacableFixed 是在 PLACABLE 的基础上多了 width height size 之类的, 是固定大小的. 例如住房
是否可以放置都是 PlacableFixed 自己判断
```java
public abstract CharSequence placable(int tx, int ty, int rx, int ry);
public abstract CharSequence placableWhole(int tx1, int ty1);
```

PlacableMulti 是需要自己选多个 tile, 例如各种工仿.
和 Fixed 的主要区别在于判断是否能放置
```java
public abstract CharSequence isPlacable(int tx, int ty, AREA area, PLACER_TYPE type); 
// Validate entire area
public CharSequence isPlacable(AREA area, PLACER_TYPE type);
```

复制区域的 First 阶段是 Multi, Second 阶段是 Fixed

## UICopier

UICopier 是没 data 的.
可以跳过第一阶段的 First tool. 
持久化的数据可以通过调用 Source.set 读回到内存里面.
设置好 Source 之后把 tool 替换成 Second. 就会自动 render 对应的 place holder.

实际放置的时候
Second.place 会被调用 n 次, n 是单元格数量.

