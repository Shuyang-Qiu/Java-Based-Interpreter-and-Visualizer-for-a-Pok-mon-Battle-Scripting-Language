public class BattleEvent {
    public enum Type {
        TEXT,
        POKEMON_LOADED,
        SPELL
    }

    public final Type type;
    public final String message;

    public final String pokemonName;
    public final String source;
    public final int hp;
    public final int attack;
    public final int defend;

    public final String attacker;
    public final String target;
    public final String moveName;
    public final int damage;
    public final int hpBefore;
    public final int hpAfter;

    private BattleEvent(Type type, String message,
                        String pokemonName, String source, int hp, int attack, int defend,
                        String attacker, String target, String moveName,
                        int damage, int hpBefore, int hpAfter) {
        this.type = type;
        this.message = message;
        this.pokemonName = pokemonName;
        this.source = source;
        this.hp = hp;
        this.attack = attack;
        this.defend = defend;
        this.attacker = attacker;
        this.target = target;
        this.moveName = moveName;
        this.damage = damage;
        this.hpBefore = hpBefore;
        this.hpAfter = hpAfter;
    }

    public static BattleEvent text(String message) {
        return new BattleEvent(Type.TEXT, message,
                null, null, 0, 0, 0,
                null, null, null, 0, 0, 0);
    }

    public static BattleEvent pokemonLoaded(String name, String source, int hp, int attack, int defend) {
        return new BattleEvent(Type.POKEMON_LOADED, null,
                name, source, hp, attack, defend,
                null, null, null, 0, 0, 0);
    }

    public static BattleEvent spell(String attacker, String target, String moveName,
                                    int damage, int hpBefore, int hpAfter) {
        return new BattleEvent(Type.SPELL, null,
                null, null, 0, 0, 0,
                attacker, target, moveName, damage, hpBefore, hpAfter);
    }
}
