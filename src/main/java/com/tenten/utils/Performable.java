package com.tenten.performable;

public abstract class Performable {

    public Performable() {
    }

    // Run the Performable code
    public abstract void run();

    // Provide description for logging
    public abstract String description();
}
