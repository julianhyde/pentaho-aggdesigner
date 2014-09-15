/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*
* Copyright 2006 - 2013 Pentaho Corporation.  All rights reserved.
*/

package org.pentaho.aggdes.output.impl;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.aggdes.model.Aggregate;
import org.pentaho.aggdes.model.Schema;
import org.pentaho.aggdes.output.ArtifactGenerator;
import org.pentaho.aggdes.output.Output;
import org.pentaho.aggdes.output.OutputFactory;
import org.pentaho.aggdes.output.OutputService;
import org.pentaho.aggdes.output.OutputValidationException;

public class OutputServiceImpl implements OutputService {

    private Schema schema;
    
    private List<ArtifactGenerator> artifactGenerators = new ArrayList<ArtifactGenerator>();
    private List<OutputFactory> outputFactories = new ArrayList<OutputFactory>();
    
    public OutputServiceImpl() {
    }
    
    public void setOutputFactories(List<OutputFactory> outputFactories) {
        this.outputFactories = outputFactories;
    }
    
    public void setArtifactGenerators(List<ArtifactGenerator> artifactGenerators) {
        this.artifactGenerators = artifactGenerators;
    }
    
    public List<OutputFactory> getOutputFactories() {
        return outputFactories;
    }
    
    public List<ArtifactGenerator> getArtifactGenerators() {
        return artifactGenerators;
    }

    
    public OutputServiceImpl(Schema schema) {
        this();
        this.schema = schema;
    }
        
    public void setSchema(Schema schema) {
        this.schema = schema;
    }
    
    private boolean containsSupportedOutputClass(Class<? extends Object> objectClass, Class<? extends Object>[] objectClasses) {
        for (Class clazz : objectClasses) {
            if (objectClass.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }
    
    public Class<? extends ArtifactGenerator>[] getSupportedArtifactGeneratorClasses() {
        List<Class> list = new ArrayList<Class>();
        for (OutputFactory factory : outputFactories) {
            if (factory.canCreateOutput(schema)) {
                for (ArtifactGenerator generator : artifactGenerators) {
                    if (containsSupportedOutputClass(factory.getOutputClass(), generator.getSupportedOutputClasses())) {
                        if (!list.contains(generator)) {
                            list.add(generator.getClass());
                        }
                    }
                }
                break;
            }
        }
        return list.toArray(new Class[0]);
    }
    
    private boolean isOutputCompatible(Output output, Class<? extends Output>[] clazzes) {
        for (Class<?> clazz : clazzes) {
            if (clazz.isAssignableFrom(output.getClass())) {
                return true;
            }
        }
        return false;
    }
    
    private ArtifactGenerator getArtifactGenerator(Class<? extends ArtifactGenerator> artifactGenerator, Output output) {
        // for each artifact generator
        for (int i = 0; i < artifactGenerators.size(); i++) {
            // see if it implements artifactGenerator and supports the output type
            if (artifactGenerator.isAssignableFrom(artifactGenerators.get(i).getClass())
                && isOutputCompatible(output, artifactGenerators.get(i).getSupportedOutputClasses())) {
                if (artifactGenerators.get(i).canGenerate(schema, output)) {
                    return artifactGenerators.get(i);
                }
            }
        }
        return null;
    }
    
    public String getArtifact(Output output, Class<? extends ArtifactGenerator> artifactGenerator) throws OutputValidationException {
        if (output == null) {
            throw new OutputValidationException("No Output Provided");
        }
        if (artifactGenerator == null) {
            throw new OutputValidationException("No Generator Provided");
        }
        ArtifactGenerator generator = getArtifactGenerator(artifactGenerator, output);
        if (generator == null) {
            throw new OutputValidationException("Failed to locate generator of type " + artifactGenerator + " compatible with output " + output);
        }
        return generator.generate(schema, output);
    }

    public String getFullArtifact(List<? extends Output> outputs, Class<? extends ArtifactGenerator> artifactGenerator) throws OutputValidationException {

        // verify all outputs are of the same type
        if (outputs == null || outputs.size() == 0) {
            throw new OutputValidationException("No Output Provided");
        }
        if (artifactGenerator == null) {
            throw new OutputValidationException("No Generator Provided");
        }
        
        ArtifactGenerator generator = getArtifactGenerator(artifactGenerator, outputs.get(0));
        
        if (generator == null) {
            throw new OutputValidationException("Failed to locate generator of type " + artifactGenerator);
        }
        
        for (Output output : outputs) {
            if (!generator.canGenerate(schema, output)) {
                throw new OutputValidationException("Generator " + generator.getClass().getName() + " cannot generate output " + output.toString() + " .  Unable to generate full artifact.");
            }
        }
        
        return generator.generateFull(schema, outputs);
    }
   
    static int k = 0;
    
    public Output generateDefaultOutput(Aggregate aggregate) throws OutputValidationException {
        
        if (aggregate == null) {
            throw new OutputValidationException("No Aggregate Provided.");
        }
        
        for (OutputFactory factory : outputFactories) {
            if (factory.canCreateOutput(schema)) {
                return factory.createOutput(schema, aggregate);
            }
        }
        
        throw new OutputValidationException("Failed to locate Output Factory.");
    }

    public void init(Schema schema) {
      setSchema(schema);
    }
}
