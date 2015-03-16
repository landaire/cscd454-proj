package com.cscd.game.model.characters.good;

import com.cscd.game.model.classes.A_ClassMagicDamageMage;

/**
 * Sean Burright
 * Lander Brandt
 * Tony Moua
 */
public class Mage extends A_ClassMagicDamageMage
{
    public static final String TYPE = "Mage";
 public Mage()
 {
  super(new Inventory(), Mage.TYPE,300,100,25,45,.7,20);
 }
 
 public Mage(Inventory inventory, String name, int HP, int MP, int minDamage, int maxDamage, double chanceToHit, int range)
 {
  super(inventory, name, HP, MP, minDamage, maxDamage, chanceToHit, range);
 }
}
