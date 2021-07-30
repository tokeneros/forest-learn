package com.dtflys.forest.mapping;

/**
 * @author gongjun[jun.gong@thebeastshop.com]
 * @since 2018-01-09 17:44
 */
public class MappingDouble extends MappingExpr {

    private final double number;

    public MappingDouble(double number) {
        super(Token.DOUBLE);
        this.number = number;
    }

    @Override
    public Object render(Object[] args) {
        return number;
    }

    @Override
    public boolean isIterateVariable() {
        return false;
    }

    public double getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "[Double: " + number + "]";
    }

}
