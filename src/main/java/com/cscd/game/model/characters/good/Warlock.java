package com.cscd.game.model.characters.good;

import com.cscd.game.model.classes.A_ClassMagicDamageWarlock;

/**
 * Sean Burright
 * Lander Brandt
 * Tony Moua
 */
public class Warlock extends A_ClassMagicDamageWarlock
{
    public static final String TYPE = "Warlock";
 public Warlock()
 {
  super(Warlock.TYPE,400,100,20,40,.7,20);
 }
 public Warlock(String name, int HP, int MP, int minDamage, int maxDamage, double chanceToHit, int range)
 {
  super(name, HP, MP, minDamage, maxDamage, chanceToHit, range);
 }
}