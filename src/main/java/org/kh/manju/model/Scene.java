package org.kh.manju.model;

import java.util.List;

public record Scene(
        int index,
        String location,
        String goal,
        String conflictBeat,
        List<Panel> panels
) {
}
