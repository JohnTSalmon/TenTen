package com.tenten.logger;

import com.tenten.utils.Colours;

public interface LoggerInterface {

    void log(String ... args );
    void log(Colours colour, String text);
    void warn( String text);
}
