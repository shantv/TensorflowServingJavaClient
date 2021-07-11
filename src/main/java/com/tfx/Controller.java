package com.tfx;

import java.io.IOException;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Controller {
	@Autowired TensorflowService ts;

	@GetMapping("/foo")
	public Object foo() {
		return "hello";
	}

	@GetMapping(path = "/test")
	public Object test() throws IOException {
		return ts.test();
	}
}
