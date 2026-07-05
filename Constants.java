package testjade;

import jade.core.AID;
import java.util.ArrayList;
import java.util.List;

public interface Constants {
    
    AID MAGIC_NEPAL = new AID("MagicNepal", AID.ISLOCALNAME);
    AID TIGER_NEPAL = new AID("TigerNepal", AID.ISLOCALNAME);
    AID TREK_NEPAL = new AID("TrekNepal", AID.ISLOCALNAME);
    AID ADVENTURE_NEPAL = new AID("AdventureNepal", AID.ISLOCALNAME);
    AID TREE_SISTERS = new AID("TreeSisters", AID.ISLOCALNAME);
    
    AID BUYER = new AID("Lola", AID.ISLOCALNAME);

    String AGENT_INFO = " Agent :: ";
    
    List<AID> FSMSELLER = new ArrayList<>(List.of(
        MAGIC_NEPAL, TIGER_NEPAL, TREK_NEPAL, ADVENTURE_NEPAL, TREE_SISTERS
    ));
}