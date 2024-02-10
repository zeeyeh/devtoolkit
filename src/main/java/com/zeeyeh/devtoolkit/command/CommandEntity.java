package com.zeeyeh.devtoolkit.command;

import com.zeeyeh.devtoolkit.annotation.Commander;

import java.util.Set;

public class CommandEntity {
    private String name;
    private Class<?> instance;
    private Set<SubCommandEntity> subCommands;
    private Commander description;
    private boolean showHelp;

    public CommandEntity() {
    }

    public CommandEntity(String name, Class<?> instance, Set<SubCommandEntity> subCommands, Commander description, boolean showHelp) {
        this.name = name;
        this.instance = instance;
        this.subCommands = subCommands;
        this.description = description;
        this.showHelp = showHelp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getInstance() {
        return instance;
    }

    public void setInstance(Class<?> instance) {
        this.instance = instance;
    }

    public Set<SubCommandEntity> getSubCommands() {
        return subCommands;
    }

    public void setSubCommands(Set<SubCommandEntity> subCommands) {
        this.subCommands = subCommands;
    }

    public Commander getDescription() {
        return description;
    }

    public void setDescription(Commander description) {
        this.description = description;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }
}
