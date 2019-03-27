## Build the Java Server
 1. Navigate to Java_API/Car-Grpc
 2. Run mvn clean install
 3. Install proto compiler for your platform, located at -- https://github.com/protocolbuffers/protobuf/releases
 4. Run the proto compiler using the command: 
     * protoc --proto_path=/path/to/proto/queryEngine.proto --java_out=/.../Java_API/Car-Grpc/target / /path/to/proto/queryEngine.proto

## Running the Java Server
* The index file is required to run the Java Server
* The index and other project files like the reduced size vocab file, glove embeddings and glove terms, and the generated global elmo embeddings are all available at the link shown below:
* https://console.cloud.google.com/storage/browser/2196151p-project-files
1. The Server can be run by running the Jar-With-Dependencies file.
    * This file takes the index path and all.tree.qrels path as arguments
    * java -jar cargrpc-1.0-SNAPSHOT-jar-with-dependencies.jar "/path/to/index/" "/.../Java_API/Car-Grpc/src/main/resources/all.tree.qrels"

## Running the Flask Application
The compiled proto files for the Flask Webapp are already included in the zip.
However, if you would like to regenerate these, this can be done using the same compiler and the command below in the directory containing app.py

protoc --proto_path=./queryEngine.proto --python_out=../ ./queryEngine.proto

1.  The webapp can be run using the command below and is accessible from the localhost address provided in the terminal:
    * flask run

## Running Evaluation
Without running the embedding notebooks it is still possible to run the evaluation with the files included.
Most of the functions in the embedding notebook take a long time to run and as such it is recommended that the files contained in the output directories are used as part of the pipeline. i.e. due to long running times it is not recommended to run the notebook.

However, if they will be run, you must first download the Glove-word2vec file. This file is found in the bucket linked above.

1. Run a jupyter notebook and open Logic.ipynb. 
    * It is possible to run all cells in this file to generate Glove Embedding Files and ELMo Embedding Files.
    * However, the ELMo embedding files take 60 seconds per query so these files are stored in the folder src/main/resources/data/Local_ELMo for use in the pipeline.
    * The Glove embedding files are stored in src/main/resources/data/topIN.
    * 
In order to run the evaluations, the project can be loaded into an IDE, then the ELMo and Glove pipelines can be run. This will allow you to generate queries, batch retrieve and evaluate them.

The arguments for both pipelines are formatted

* ELMoPipeline Average/Independent/NoContext/Global numExpansionTerms train/test "Path/To/Index"
* GlovePipeline train/test numExpansionTerms "Path/To/Index"
