package ph.codeia.signal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This file is a part of the vanilla project.
 */

/**
 * Unlinks a group of links at the same time.
 */
public class Links implements Channel.Link {

    public static Links of(Channel.Link... links) {
        Links result = new Links();
        result.links.addAll(Arrays.asList(links));
        return result;
    }

    private final List<Channel.Link> links = new ArrayList<>();

    public Links add(Channel.Link link) {
        links.add(link);
        return this;
    }

    @Override
    public void unlink() {
        for (Channel.Link l : links) {
            l.unlink();
        }
        links.clear();
    }

}
