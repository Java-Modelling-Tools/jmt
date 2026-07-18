package jmt.jmva.analytical.solvers.linearSystem.btf.macroBlocks;

import jmt.jmva.analytical.solvers.basis.CoMoMBasis;
import jmt.jmva.analytical.solvers.dataStructures.QNModel;
import jmt.jmva.analytical.solvers.exceptions.InconsistentLinearSystemException;
import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.linearSystem.btf.Position;
import jmt.jmva.analytical.solvers.linearSystem.btf.microBlocks.CMicroBlock;
import jmt.jmva.analytical.solvers.linearSystem.btf.microBlocks.MicroBlock;

/**
 * This class implements the Macro Blocks of the Carry Forward Equations
 * 
 * The Factory Methods newMicroBlock and subBlockCopy are implemented to return the correct 
 * block type in the parallel hierarchy.
 * 
 * Type 2a Micro Blocks are selected for lower class linear systems
 * 
 * @author Jack Bradshaw
 */
public class CMacroBlock extends MacroBlock {
	
	public CMacroBlock(QNModel qnm, CoMoMBasis basis, Position position, int h) throws InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, position, h);		
		selection_policy = new TypeTwoABlocks(qnm, this); 
	}

	public CMacroBlock(MacroBlock full_block, int current_class) {
		super(full_block, current_class);		
	}
	
	@Override
	protected MacroBlock subBlockCopy(int current_class) {
		return new CMacroBlock(this, current_class);
	}

	@Override
	protected MicroBlock newMicroBlock(Position block_position, int h) throws InternalErrorException {
		return new CMicroBlock(qnm, basis, block_position, h);
	}	

}
