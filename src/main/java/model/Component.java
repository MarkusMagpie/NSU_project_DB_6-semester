package model;

public class Component {
    private int componentId;
    private String name;
    private int shelfLife;
    private double criticalLevel;
    private double price;

    public Component(int componentId, String name, int shelfLife, double criticalLevel, double price) {
        this.componentId = componentId;
        this.name = name;
        this.shelfLife = shelfLife;
        this.criticalLevel = criticalLevel;
        this.price = price;
    }

    public int getComponentId() { return componentId; }
    public String getName() { return name; }
    public int getShelfLife() { return shelfLife; }
    public double getCriticalLevel() { return criticalLevel; }
    public double getPrice() { return price; }
}