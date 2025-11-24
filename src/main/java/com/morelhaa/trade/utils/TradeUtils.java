package com.morelhaa.trade.utils;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase de utilidades para el sistema de trades
 * Contiene métodos helper y funciones comunes
 */
public class TradeUtils {

    /**
     * Verifica si un jugador tiene espacio suficiente en su inventario
     * @param player Jugador a verificar
     * @param items Items que necesita recibir
     * @return true si tiene espacio suficiente
     */
    public static boolean hasInventorySpace(Player player, List<Item> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }

        // Contar slots vacíos
        int emptySlots = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            Item item = player.getInventory().getItem(i);
            if (item == null || item.getId() == 0) {
                emptySlots++;
            }
        }

        // Contar items que necesitan slots nuevos
        int requiredSlots = 0;
        for (Item item : items) {
            if (item != null && item.getId() != 0) {
                // Verificar si puede stackearse con items existentes
                boolean canStack = false;
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    Item existing = player.getInventory().getItem(i);
                    if (existing != null && existing.getId() == item.getId()
                            && existing.getDamage() == item.getDamage()
                            && existing.getCount() < existing.getMaxStackSize()) {

                        int remaining = existing.getMaxStackSize() - existing.getCount();
                        if (remaining >= item.getCount()) {
                            canStack = true;
                            break;
                        }
                    }
                }

                if (!canStack) {
                    requiredSlots++;
                }
            }
        }

        return emptySlots >= requiredSlots;
    }

    /**
     * Verifica la distancia entre dos jugadores
     * @param player1 Primer jugador
     * @param player2 Segundo jugador
     * @return Distancia en bloques
     */
    public static double getDistance(Player player1, Player player2) {
        if (player1 == null || player2 == null) {
            return Double.MAX_VALUE;
        }

        if (!player1.getLevel().getName().equals(player2.getLevel().getName())) {
            return Double.MAX_VALUE;
        }

        return player1.getLocation().distance(player2.getLocation());
    }

    /**
     * Reproduce un sonido a un jugador
     * @param player Jugador que escuchará el sonido
     * @param sound Sonido a reproducar
     */
    public static void playSound(Player player, Sound sound) {
        if (player != null && player.isOnline()) {
            player.getLevel().addSound(player.getLocation(), sound);
        }
    }

    /**
     * Reproduce un sonido a ambos jugadores
     * @param player1 Primer jugador
     * @param player2 Segundo jugador
     * @param sound Sonido a reproducir
     */
    public static void playSoundToPlayers(Player player1, Player player2, Sound sound) {
        playSound(player1, sound);
        playSound(player2, sound);
    }

    /**
     * Obtiene todos los items no vacíos de una lista
     * @param items Lista de items
     * @return Lista filtrada sin items vacíos
     */
    public static List<Item> filterEmptyItems(List<Item> items) {
        List<Item> filtered = new ArrayList<>();
        if (items != null) {
            for (Item item : items) {
                if (item != null && item.getId() != 0) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }

    /**
     * Clona una lista de items
     * @param items Lista original
     * @return Lista clonada
     */
    public static List<Item> cloneItems(List<Item> items) {
        List<Item> cloned = new ArrayList<>();
        if (items != null) {
            for (Item item : items) {
                if (item != null && item.getId() != 0) {
                    cloned.add(item.clone());
                }
            }
        }
        return cloned;
    }

    /**
     * Valida que un jugador esté en condiciones de comerciar
     * @param player Jugador a validar
     * @return true si puede comerciar
     */
    public static boolean isPlayerValid(Player player) {
        return player != null
                && player.isOnline()
                && player.isAlive()
                && !player.isSleeping();
    }

    /**
     * Valida que ambos jugadores estén en condiciones de comerciar
     * @param player1 Primer jugador
     * @param player2 Segundo jugador
     * @return true si ambos pueden comerciar
     */
    public static boolean arePlayersValid(Player player1, Player player2) {
        return isPlayerValid(player1) && isPlayerValid(player2);
    }

    /**
     * Da items a un jugador de forma segura
     * Si el inventario está lleno, dropea los items
     * @param player Jugador que recibirá los items
     * @param items Items a dar
     */
    public static void giveItemsSafely(Player player, List<Item> items) {
        if (player == null || items == null || items.isEmpty()) {
            return;
        }

        for (Item item : items) {
            if (item != null && item.getId() != 0) {
                // Intentar añadir al inventario
                if (!player.getInventory().canAddItem(item)) {
                    // Si no cabe, dropear el item
                    player.getLevel().dropItem(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }
            }
        }
    }

    /**
     * Formatea un número en formato legible
     * @param number Número a formatear
     * @return String formateado
     */
    public static String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    /**
     * Verifica si un slot es un slot de separador
     * @param slot Número de slot
     * @return true si es un separador
     */
    public static boolean isSeparatorSlot(int slot) {
        // Columna del medio del cofre doble (slots 4, 13, 22, 31, 40, 49)
        return slot == 4 || slot == 13 || slot == 22 ||
                slot == 31 || slot == 40 || slot == 49;
    }

    /**
     * Verifica si un slot pertenece al lado izquierdo
     * @param slot Número de slot
     * @return true si está en el lado izquierdo
     */
    public static boolean isLeftSide(int slot) {
        int column = slot % 9;
        return column < 4;
    }

    /**
     * Verifica si un slot pertenece al lado derecho
     * @param slot Número de slot
     * @return true si está en el lado derecho
     */
    public static boolean isRightSide(int slot) {
        int column = slot % 9;
        return column > 4;
    }

    /**
     * Convierte un item en string para debug
     * @param item Item a convertir
     * @return String descriptivo
     */
    public static String itemToString(Item item) {
        if (item == null || item.getId() == 0) {
            return "AIR";
        }
        return item.getName() + " x" + item.getCount() + " (ID:" + item.getId() + ":" + item.getDamage() + ")";
    }

    /**
     * Verifica si dos items son similares (mismo ID y damage)
     * @param item1 Primer item
     * @param item2 Segundo item
     * @return true si son similares
     */
    public static boolean areItemsSimilar(Item item1, Item item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        return item1.getId() == item2.getId() && item1.getDamage() == item2.getDamage();
    }
}