package org.workcraft.formula.workers;

import org.workcraft.formula.*;

public final class DumbBooleanWorker implements BooleanWorker {

    private static DumbBooleanWorker instance;

    private DumbBooleanWorker() {
    }

    public static DumbBooleanWorker getInstance() {
        if (instance == null) {
            instance = new DumbBooleanWorker();
        }
        return instance;
    }

    @Override
    public BooleanFormula zero() {
        return Zero.getInstance();
    }

    @Override
    public BooleanFormula one() {
        return One.getInstance();
    }

    @Override
    public BooleanFormula not(BooleanFormula x) {
        return new Not(x);
    }

    @Override
    public BooleanFormula and(BooleanFormula x, BooleanFormula y) {
        return new And(x, y);
    }

    @Override
    public BooleanFormula or(BooleanFormula x, BooleanFormula y) {
        return new Or(x, y);
    }

    @Override
    public BooleanFormula xor(BooleanFormula x, BooleanFormula y) {
        return new Xor(x, y);
    }

    @Override
    public BooleanFormula imply(BooleanFormula x, BooleanFormula y) {
        return new Imply(x, y);
    }

    @Override
    public BooleanFormula iff(BooleanFormula x, BooleanFormula y) {
        return new Iff(x, y);
    }

}
