/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.utils.misc;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/** Localizes Meteor command feedback without changing Brigadier's parser or user input. */
public final class CommandErrors {
    private CommandErrors() {}

    public static String message(CommandSyntaxException error) {
        var builtins = CommandSyntaxException.BUILT_IN_EXCEPTIONS;
        var type = error.getType();
        String label = null;
        if (type == builtins.dispatcherUnknownCommand()) label = "Unknown command";
        else if (type == builtins.dispatcherUnknownArgument()) label = "Unknown or incomplete argument";
        else if (type == builtins.dispatcherExpectedArgumentSeparator()) label = "Expected a space between arguments";
        else if (type == builtins.readerExpectedStartOfQuote()) label = "Expected a starting quote";
        else if (type == builtins.readerExpectedEndOfQuote()) label = "Unclosed quoted string";
        else if (type == builtins.readerInvalidEscape()) label = "Invalid escape sequence";
        else if (type == builtins.readerInvalidBool() || type == builtins.readerExpectedBool()) label = "Expected true or false";
        else if (type == builtins.readerInvalidInt() || type == builtins.readerExpectedInt()
            || type == builtins.readerInvalidLong() || type == builtins.readerExpectedLong()) label = "Expected an integer";
        else if (type == builtins.readerInvalidDouble() || type == builtins.readerExpectedDouble()
            || type == builtins.readerInvalidFloat() || type == builtins.readerExpectedFloat()) label = "Expected a number";
        else if (type == builtins.doubleTooLow() || type == builtins.floatTooLow()
            || type == builtins.integerTooLow() || type == builtins.longTooLow()) label = "Number is below the allowed minimum";
        else if (type == builtins.doubleTooHigh() || type == builtins.floatTooHigh()
            || type == builtins.integerTooHigh() || type == builtins.longTooHigh()) label = "Number exceeds the allowed maximum";
        else if (type == builtins.literalIncorrect() || type == builtins.readerExpectedSymbol()) label = "Expected a different command token";
        else if (type == builtins.dispatcherParseException()) label = "Could not parse the command";

        String result = Localization.text(label == null ? error.getRawMessage().getString() : label);
        if (error.getCursor() >= 0) result += String.format(Localization.text(" at position %d"), error.getCursor());
        if (error.getContext() != null) result += ": " + error.getContext();
        return result;
    }
}
