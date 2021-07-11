package com.tfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.tensorflow.example.BytesList;
import org.tensorflow.example.Example;
import org.tensorflow.example.Feature;
import org.tensorflow.example.Features;
import org.tensorflow.example.FloatList;
import org.tensorflow.example.Int64List;
import org.tensorflow.framework.DataType;
import org.tensorflow.framework.TensorProto;
import org.tensorflow.framework.TensorShapeProto;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import tensorflow.serving.Model;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;

@Service
public class TensorflowService {
	private static Map<String, Feature> mapToFeatureMap(Map<String, Object> dataMap) {

		Map<String, Feature> resultMap = new HashMap<String, Feature>();
		for (String key : dataMap.keySet()) {
			// // data1 =
			// {"SepalLength":5.1,"SepalWidth":3.3,"PetalLength":1.7,"PetalWidth":0.5}
//			System.out.println(key + " " + dataMap.get(key).getClass().getSimpleName());
			String clazz = dataMap.get(key).getClass().getSimpleName();
			if (clazz.equals("Float")) {
				FloatList floatList = FloatList.newBuilder().addValue((Float)dataMap.get(key)).build();
				Feature feature = Feature.newBuilder().setFloatList(floatList).build();
				resultMap.put(key, feature);
			}
			if (clazz.equals("Integer")) {
				Int64List floatList = Int64List.newBuilder().addValue((Integer)dataMap.get(key)).build();
				Feature feature = Feature.newBuilder().setInt64List(floatList).build();
				resultMap.put(key, feature);
			}
			if (clazz.equals("String")) {
				BytesList floatList = BytesList.newBuilder().addValue(ByteString.copyFromUtf8((String)dataMap.get(key))).build();
				Feature feature = Feature.newBuilder().setBytesList(floatList).build();
				resultMap.put(key, feature);				
			}
		}
		return resultMap;
	}

	public static Example createExample() {
		Features.Builder featuresBuilder = Features.newBuilder();
        Random r = new Random();
        
		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("fare", r.nextFloat());
		dataMap.put("payment_type", "tst");
		dataMap.put("tips", r.nextFloat());
		dataMap.put("trip_miles", r.nextFloat());
		dataMap.put("trip_start_day", r.nextInt());
		dataMap.put("trip_start_hour", r.nextInt());
		dataMap.put("trip_start_month", r.nextInt());
		dataMap.put("trip_start_timestamp", r.nextInt());

		Map<String, Feature> featuresMap = mapToFeatureMap(dataMap);
		featuresBuilder.putAllFeature(featuresMap);

		Features features = featuresBuilder.build();
		Example.Builder exampleBuilder = Example.newBuilder();
		exampleBuilder.setFeatures(features);
		return exampleBuilder.build();
	}

	public Object test() {
		try {
			String host = "127.0.0.1";
			int port = 8500;
			long start = System.currentTimeMillis();
			ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
//		            // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
//		            // needing certificates.
					.usePlaintext().build();
			PredictionServiceGrpc.PredictionServiceBlockingStub blockingStub = PredictionServiceGrpc
					.newBlockingStub(channel);

//			com.google.protobuf.Int64Value version = com.google.protobuf.Int64Value.newBuilder()
//					.setValue(1626026906)
//					.build();

			Model.ModelSpec modelSpec = Model.ModelSpec.newBuilder()
					.setName("taxi")
//					.setVersion(version)
					.setSignatureName("serving_default")
					.build();

	        List<ByteString> exampleList = new ArrayList<ByteString>();
	        int batch_size = 4096;
	        for ( int i = 0 ; i < batch_size; i++ ) {
		        exampleList.add(createExample().toByteString());
	        }	        
			TensorShapeProto.Dim featureDim = TensorShapeProto.Dim.newBuilder().setSize(exampleList.size()).build();
			TensorShapeProto shapeProto = TensorShapeProto.newBuilder().addDim(featureDim).build();
			TensorProto tensorProto = TensorProto.newBuilder()
					.addAllStringVal(exampleList)
					.setDtype(DataType.DT_STRING)
					.setTensorShape(shapeProto)
					.build();

			Predict.PredictRequest request = Predict.PredictRequest.newBuilder().setModelSpec(modelSpec)
					.putInputs("examples", tensorProto).build();
			Predict.PredictResponse response = blockingStub.predict(request);
			channel.shutdown();
			
			TensorProto pred = response.getOutputsMap().get("output_0");
			long end = System.currentTimeMillis();
			long diff = end - start;
			System.out.println(pred.getSerializedSize() + " took : " + diff);
			return pred.getFloatValList();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
