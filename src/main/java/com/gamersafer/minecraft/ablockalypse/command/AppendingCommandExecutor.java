package com.gamersafer.minecraft.ablockalypse.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppendingCommandExecutor<C extends CommandExecutor & TabCompleter> implements CommandExecutor, TabCompleter {

    private final C wrapped;
    private final Command parent;

    public AppendingCommandExecutor(C wrapped, Command parent) {
        this.wrapped = wrapped;
        this.parent = parent;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> arguments = new ArrayList<>();
        arguments.add(parent.getName());
        Collections.addAll(arguments, args);

        return this.wrapped.onCommand(sender, command, label, arguments.toArray(new String[0]));
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> arguments = new ArrayList<>();
        arguments.add(parent.getName());
        Collections.addAll(arguments, args);

        return this.wrapped.onTabComplete(sender, command, label, arguments.toArray(new String[0]));
    }
}
