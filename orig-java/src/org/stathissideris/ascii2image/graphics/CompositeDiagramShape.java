/*
 * DiTAA - Diagrams Through Ascii Art
 *
 * Copyright (C) 2004 Efstathios Sideris
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.stathissideris.ascii2image.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.stathissideris.ascii2image.core.DebugUtils;
import org.stathissideris.ascii2image.text.CellSet;
import org.stathissideris.ascii2image.text.TextGrid;

/**
 *
 * @author Efstathios Sideris
 */
public class CompositeDiagramShape extends DiagramComponent {

	private static final boolean DEBUG = false;

	private ArrayList<DiagramShape> shapes = new ArrayList<DiagramShape>();

	public static DiagramComponent createOpenFromBoundaryCells(final TextGrid grid, final CellSet boundaryCells,
			final int cellWidth, final int cellHeight, boolean allRound) {

		if (boundaryCells.getType(grid) != CellSet.TYPE_OPEN) {
			throw new IllegalArgumentException("This shape is closed and cannot be handled by this method");
		}
		if (boundaryCells.size() == 0) {
			return null;
		}

		CompositeDiagramShape compositeShape = new CompositeDiagramShape();
		TextGrid workGrid = new TextGrid(grid.getWidth(), grid.getHeight());
		grid.copyCellsTo(boundaryCells, workGrid);

		if (DEBUG) {
			System.out.println("Making composite shape from grid:");
			workGrid.printDebug();
		}

		CellSet visitedCells = new CellSet();

		List<DiagramShape> shapes = new ArrayList<DiagramShape>(100);

		for (TextGrid.Cell cell : boundaryCells) {
			if (workGrid.isLinesEnd(cell)) {
				CellSet nextCells = workGrid.followCell(cell);
				shapes.addAll(growEdgesFromCell(workGrid, cellWidth, cellHeight, allRound, nextCells
						.getFirst(), cell, visitedCells));
				break;
			}
		}

		//dashed shapes should "infect" the rest of the shapes
		boolean dashedShapeExists = false;
		for (DiagramShape shape : shapes) {
			if (shape.isStrokeDashed()) {
				dashedShapeExists = true;
			}
		}

		for (DiagramShape shape : shapes) {
			if (dashedShapeExists) {
				shape.setStrokeDashed(true);
			}
			compositeShape.addToShapes(shape);
		}

		return compositeShape;
	}

	private static List<DiagramShape> growEdgesFromCell(TextGrid workGrid, final int cellWidth,
			final int cellHeight, boolean allRound, TextGrid.Cell cell, TextGrid.Cell previousCell,
			CellSet visitedCells) {

		List<DiagramShape> result = new ArrayList<DiagramShape>(50);

		visitedCells.add(previousCell);

		DiagramShape shape = new DiagramShape();

		shape.addToPoints(makePointForCell(previousCell, workGrid, cellWidth, cellHeight, allRound));
		if (DEBUG) {
			System.out.println("point at " + previousCell + " (call from line: "
					+ DebugUtils.getLineNumber() + ")");
		}
		if (workGrid.cellContainsDashedLineChar(previousCell)) {
			shape.setStrokeDashed(true);
		}

		boolean finished = false;
		while (!finished) {
			visitedCells.add(cell);
			if (workGrid.isPointCell(cell)) {
				if (DEBUG) {
					System.out.println("point at " + cell + " (call from line: "
							+ DebugUtils.getLineNumber() + ")");
				}
				shape.addToPoints(makePointForCell(cell, workGrid, cellWidth, cellHeight, allRound));
			}

			if (workGrid.cellContainsDashedLineChar(cell)) {
				shape.setStrokeDashed(true);
			}

			if (workGrid.isLinesEnd(cell)) {
				finished = true;
				if (DEBUG) {
					System.out.println("finished shape");
				}
			}

			CellSet nextCells = workGrid.followCell(cell, previousCell);
			if (nextCells.size() == 1) {
				previousCell = cell;
				cell = nextCells.getFirst();
				if (DEBUG) {
					System.out.println("tracing at " + cell + " (call from line: "
							+ DebugUtils.getLineNumber() + ")");
				}
			} else if (nextCells.size() > 1 || nextCells.size() == 0) {//3- or 4- way intersection
				finished = true;
				for (TextGrid.Cell nextCell : nextCells) {
					result.addAll(growEdgesFromCell(workGrid, cellWidth, cellHeight, allRound,
							nextCell, cell, visitedCells));
				}
			}
		}

		result.add(shape);
		return result;
	}

	/**
	 * Returns a new diagram component with the lines of this CompositeDiagramShape connected. It can a composite or simple shape
	 *
	 * @return
	 */
	public DiagramComponent connectLines() {
		CompositeDiagramShape result = new CompositeDiagramShape();

		//find all lines
		ArrayList<DiagramShape> lines = new ArrayList<DiagramShape>();
		Iterator it = shapes.iterator();
		while (it.hasNext()) {
			DiagramShape shape = (DiagramShape) it.next();
			if (shape.getPoints().size() == 2) {
				lines.add(shape);
			}
		}

		it = lines.iterator();
		while (it.hasNext()) {
			DiagramShape line1 = (DiagramShape) it.next();
			Iterator it2 = lines.iterator();
			while (it2.hasNext()) {
				DiagramShape line2 = (DiagramShape) it.next();
				ShapePoint commonPoint = null;
				if (line1.getPoint(0).equals(line2.getPoint(0))) {
					commonPoint = line1.getPoint(0);
					line1.getPoint(1);
					line2.getPoint(1);
				}
				if (line1.getPoint(0).equals(line2.getPoint(1))) {
					commonPoint = line1.getPoint(0);
					line1.getPoint(1);
					line2.getPoint(0);
				}
				if (line1.getPoint(1).equals(line2.getPoint(0))) {
					commonPoint = line1.getPoint(1);
					line1.getPoint(0);
					line2.getPoint(1);
				}
				if (line1.getPoint(1).equals(line2.getPoint(1))) {
					commonPoint = line1.getPoint(1);
					line1.getPoint(0);
					line2.getPoint(0);
				}
				if (commonPoint != null) {

				}
			}
		}

		return result;
	}

	public void connectEndsToAnchors(TextGrid grid, GraphicalGrid diagram) {
		Iterator<DiagramShape> it = shapes.iterator();
		while (it.hasNext()) {
			DiagramShape shape = it.next();
			if (!shape.isClosed()) {
				shape.connectEndsToAnchors(grid, diagram);
			}
		}
	}

	private void addToShapes(DiagramShape shape) {
		shapes.add(shape);
	}

	private Iterator getShapesIterator() {
		return shapes.iterator();
	}

	public void scale(float factor) {
		Iterator it = getShapesIterator();
		while (it.hasNext()) {
			DiagramShape shape = (DiagramShape) it.next();
			shape.scale(factor);
		}
	}

	/**
	 * @return
	 */
	public ArrayList<DiagramShape> getShapes() {
		return shapes;
	}

}
