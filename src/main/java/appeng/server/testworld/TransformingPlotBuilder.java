package appeng.server.testworld;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.function.Consumer;
import java.util.function.Function;

class TransformingPlotBuilder implements PlotBuilder {
    private final Plot plot;
    private final Function<BoundingBox, BoundingBox> transform;

    TransformingPlotBuilder(Plot plot, Function<BoundingBox, BoundingBox> transform) {
        this.plot = plot;
        this.transform = transform;
    }

    @Override
    public void addBuildAction(BuildAction action) {
        plot.addBuildAction(action);
    }

    @Override
    public void addPostBuildAction(PostBuildAction action) {
        plot.addPostBuildAction(action);
    }

    @Override
    public void addPostInitAction(PostBuildAction action) {
        plot.addPostInitAction(action);
    }

    @Override
    public BoundingBox bb(String def) {
        return transform.apply(plot.bb(def));
    }

    @Override
    public PlotBuilder transform(Function<BoundingBox, BoundingBox> transform) {
        return new TransformingPlotBuilder(this.plot, this.transform.andThen(transform));
    }

    @Override
    public Test test(Consumer<PlotTestHelper> assertion) {
        return plot.test(assertion);
    }
}
