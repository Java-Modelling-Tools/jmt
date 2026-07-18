package jmt.jmva.analytical.solvers.linearSystem.btf.topLevelBlocks;

import jmt.jmva.analytical.solvers.basis.CoMoMBasis;
import jmt.jmva.analytical.solvers.dataStructures.QNModel;
import jmt.jmva.analytical.solvers.exceptions.BTFMatrixErrorException;
import jmt.jmva.analytical.solvers.exceptions.InconsistentLinearSystemException;
import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.linearSystem.btf.Position;
import jmt.jmva.analytical.solvers.linearSystem.btf.macroBlocks.B1MacroBlock;
import jmt.jmva.analytical.solvers.linearSystem.btf.macroBlocks.MacroBlock;

/**
 * This class implements the B1 Block of the B matrix.
 * 
 * The Factory Methods newMacroBlock and subBlockCopy are implemented to return the correct 
 * block type in the parallel hierarchy.
 * 
 * Type 1 Macro Blocks are selected for lower class linear systems
 * 
 * @author Jack Bradshaw
 */
public class B1Block extends TopLevelBlock {

	public B1Block(QNModel qnm, CoMoMBasis basis) //TODO why two?
			throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, new Position(0,0));	
		selection_policy = new TypeOneBlocks(qnm, this, current_class);
	}
	
	public B1Block(QNModel qnm, CoMoMBasis basis, Position position)
			throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, position);	
		selection_policy = new TypeOneBlocks(qnm, this, current_class);
	}

	public B1Block(B1Block full_block, int current_class) {
		super(full_block, current_class);
	}
	
	@Override
	protected TopLevelBlock subBlockCopy(int current_class) throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		return new B1Block(this, current_class);
	}

	@Override
	protected MacroBlock newMacroBlock(Position block_position, int h) throws InternalErrorException, InconsistentLinearSystemException {
		return new B1MacroBlock(qnm, basis, block_position, h);
	}
}
