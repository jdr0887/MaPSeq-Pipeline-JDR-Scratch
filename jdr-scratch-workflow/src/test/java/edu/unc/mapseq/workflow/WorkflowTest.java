package edu.unc.mapseq.workflow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Test;
import org.renci.jlrm.condor.CondorJob;
import org.renci.jlrm.condor.CondorJobBuilder;
import org.renci.jlrm.condor.CondorJobEdge;
import org.renci.jlrm.condor.ext.CondorDOTExporter;

import edu.unc.mapseq.module.core.CatCLI;
import edu.unc.mapseq.module.core.EchoCLI;

public class WorkflowTest {

    @Test
    public void createDot() {

        DirectedGraph<CondorJob, CondorJobEdge> graph = new DefaultDirectedGraph<CondorJob, CondorJobEdge>(
                CondorJobEdge.class);

        int count = 0;

        // new job
        CondorJob helloJob = new CondorJobBuilder()
                .name(String.format("%s_%d", EchoCLI.class.getSimpleName(), ++count))
                .addArgument(EchoCLI.GREETING, "Hello").addArgument(EchoCLI.OUTPUT, "hello.txt").build();
        graph.addVertex(helloJob);

        // new job
        CondorJob worldJob = new CondorJobBuilder()
                .name(String.format("%s_%d", EchoCLI.class.getSimpleName(), ++count))
                .addArgument(EchoCLI.GREETING, "World").addArgument(EchoCLI.OUTPUT, "world.txt").build();
        graph.addVertex(worldJob);

        // new job
        CondorJob catJob = new CondorJobBuilder().name(String.format("%s_%d", CatCLI.class.getSimpleName(), ++count))
                .addArgument(CatCLI.FILES, "hello.txt").addArgument(CatCLI.FILES, "world.txt")
                .addArgument(CatCLI.OUTPUT, "final.txt").build();
        graph.addVertex(catJob);
        graph.addEdge(helloJob, catJob);
        graph.addEdge(worldJob, catJob);

        VertexNameProvider<CondorJob> vnpId = new VertexNameProvider<CondorJob>() {
            @Override
            public String getVertexName(CondorJob job) {
                return job.getName();
            }
        };

        VertexNameProvider<CondorJob> vnpLabel = new VertexNameProvider<CondorJob>() {
            @Override
            public String getVertexName(CondorJob job) {
                return job.getName();
            }
        };

        CondorDOTExporter<CondorJob, CondorJobEdge> dotExporter = new CondorDOTExporter<CondorJob, CondorJobEdge>(
                vnpId, vnpLabel, null, null, null, null);
        File srcSiteResourcesImagesDir = new File("src/site/resources/images");
        if (!srcSiteResourcesImagesDir.exists()) {
            srcSiteResourcesImagesDir.mkdirs();
        }
        File dotFile = new File(srcSiteResourcesImagesDir, "workflow.dag.dot");
        try {
            FileWriter fw = new FileWriter(dotFile);
            dotExporter.export(fw, graph);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
