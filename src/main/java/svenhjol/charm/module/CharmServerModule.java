package svenhjol.charm.module;

public abstract class CharmServerModule {
    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void register() {
        // run on dedicated server, even if module disabled
    }

    public void init() {
        // run on dedicated server, only if module enabled
    }
}
