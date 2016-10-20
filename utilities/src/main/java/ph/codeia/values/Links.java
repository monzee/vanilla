package ph.codeia.values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This file is a part of the vanilla project.
 */

public class Links implements Channel.Link {

    public static Links of(Channel.Link... links) {
        return new Links().add(links);
    }

    private final List<Channel.Link> links = new ArrayList<>();

    public Links add(Channel.Link link) {
        links.add(link);
        return this;
    }

    public Links add(Channel.Link... links) {
        this.links.addAll(Arrays.asList(links));
        return this;
    }

    @Override
    public void unlink() {
        for (Channel.Link l : links) {
            l.unlink();
        }
    }

}
