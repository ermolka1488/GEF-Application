package com.itemis.gef.tutorial.mindmap;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.gef.common.adapt.AdapterKey;
import org.eclipse.gef.mvc.fx.domain.HistoricizingDomain;
import org.eclipse.gef.mvc.fx.domain.IDomain;
import org.eclipse.gef.mvc.fx.viewer.IViewer;

import com.google.inject.Guice;
import com.itemis.gef.tutorial.mindmap.model.SimpleMindMap;
import com.itemis.gef.tutorial.mindmap.model.SimpleMindMapExampleFactory;
import com.itemis.gef.tutorial.mindmap.models.ItemCreationModel;
import com.itemis.gef.tutorial.mindmap.models.ItemCreationModel.Type;
import com.itemis.gef.tutorial.mindmap.visuals.MindMapNodeVisual;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SimpleMindMapApplication extends Application {

	public static HistoricizingDomain domain;

	public static void main(String[] args) {
		Application.launch(args);
	}

	private Stage primaryStage;

	private Node createButtonBar() {
		Button undoButton = new Button("Undo");
		undoButton.setDisable(true);
		undoButton.setOnAction((e) -> {
			try {
				domain.getOperationHistory().undo(domain.getUndoContext(), null, null);
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		});

		Button redoButton = new Button("Redo");
		redoButton.setDisable(true);
		redoButton.setOnAction((e) -> {
			try {
				domain.getOperationHistory().redo(domain.getUndoContext(), null, null);
			} catch (ExecutionException e1) {
				e1.printStackTrace();
			}
		});

		domain.getOperationHistory().addOperationHistoryListener((e) -> {
			IUndoContext ctx = domain.getUndoContext();
			undoButton.setDisable(!e.getHistory().canUndo(ctx));
			redoButton.setDisable(!e.getHistory().canRedo(ctx));
		});

		return new HBox(10, undoButton, redoButton);
	}

	private Node createToolPalette() {
		ItemCreationModel creationModel = getContentViewer().getAdapter(ItemCreationModel.class);

		MindMapNodeVisual graphic = new MindMapNodeVisual();
		graphic.setTitle("New Node");

		ToggleGroup toggleGroup = new ToggleGroup();

		ToggleButton createNode = new ToggleButton("", graphic);
		createNode.setToggleGroup(toggleGroup);
		createNode.selectedProperty().addListener((e, oldVal, newVal) -> {
			creationModel.setType(newVal ? Type.Node : Type.None);
		});

		ToggleButton createConn = new ToggleButton("New Connection");
		createConn.setToggleGroup(toggleGroup);
		createConn.setMaxWidth(Double.MAX_VALUE);
		createConn.setMinHeight(50);
		createConn.selectedProperty().addListener((e, oldVal, newVal) -> {
			creationModel.setType(newVal ? Type.Connection : Type.None);
		});

		creationModel.getTypeProperty().addListener((e, oldVal, newVal) -> {
			if (Type.None == newVal) {
				Toggle selectedToggle = toggleGroup.getSelectedToggle();
				if (selectedToggle != null) {
					selectedToggle.setSelected(false);
				}
			}
		});

		return new VBox(20, createNode, createConn);
	}

	private IViewer getContentViewer() {
		return domain.getAdapter(AdapterKey.get(IViewer.class, IDomain.CONTENT_VIEWER_ROLE));
	}

	private void hookViewers() {
		// creating parent pane for Canvas and button pane
		BorderPane pane = new BorderPane();

		pane.setTop(createButtonBar());
		pane.setCenter(getContentViewer().getCanvas());
		pane.setRight(createToolPalette());

		pane.setMinWidth(800);
		pane.setMinHeight(600);

		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
	}

	private void populateViewerContents() {
		SimpleMindMapExampleFactory fac = new SimpleMindMapExampleFactory();

		SimpleMindMap mindMap = fac.createComplexExample();

		IViewer viewer = getContentViewer();
		viewer.getContents().setAll(mindMap);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		SimpleMindMapModule module = new SimpleMindMapModule();
		this.primaryStage = primaryStage;
		SimpleMindMapApplication.domain = (HistoricizingDomain) Guice.createInjector(module).getInstance(IDomain.class);

		hookViewers();

		domain.activate();

		populateViewerContents();

		primaryStage.setResizable(true);
		primaryStage.setTitle("GEF Simple Mindmap");
		primaryStage.sizeToScene();
		primaryStage.show();
	}
}