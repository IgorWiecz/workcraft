package org.workcraft.plugins.fst;

import org.workcraft.annotations.IdentifierPrefix;
import org.workcraft.observation.PropertyChangedEvent;
import org.workcraft.plugins.builtin.settings.SignalCommonSettings;
import org.workcraft.plugins.fsm.Symbol;

import java.awt.*;

@IdentifierPrefix("x")
public class Signal extends Symbol {

    public static final String PROPERTY_TYPE = "Type";

    public enum Type {
        INPUT("input"),
        OUTPUT("output"),
        INTERNAL("internal"),
        DUMMY("dummy");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public Type toggle() {
            switch (this) {
            case INPUT: return OUTPUT;
            case OUTPUT: return INTERNAL;
            case INTERNAL: return INPUT;
            case DUMMY: return DUMMY;
            default: return this;
            }
        }

        public Color getColor() {
            switch (this) {
            case INPUT:    return SignalCommonSettings.getInputColor();
            case OUTPUT:   return SignalCommonSettings.getOutputColor();
            case INTERNAL: return SignalCommonSettings.getInternalColor();
            default:       return SignalCommonSettings.getDummyColor();
            }
        }
    }

    private Type type = Type.DUMMY;

    public Type getType() {
        return type;
    }

    public void setType(Type value) {
        if (type != value) {
            type = value;
            sendNotification(new PropertyChangedEvent(this, PROPERTY_TYPE));
        }
    }

    public boolean hasDirection() {
        return getType() != Type.DUMMY;
    }

}

