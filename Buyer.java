package testjade;

import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

public class Buyer extends DefaultAgent {

    @Override
    protected void setup() {
        super.setup();
        addBehaviour(new BuyerBehaviour(this));
    }

    private static class BuyerBehaviour extends FSMBehaviour {
        
        // Variables de session pour mémoriser l'état des négociations
        private int bestPrice = Integer.MAX_VALUE;
        private AID bestSeller = null;
        private final List<AID> losers = new ArrayList<>();
        private int refuseCount = 0;

        public BuyerBehaviour(Buyer a) {
            super(a);
        }

        @Override
        public void onStart() {
            super.onStart();

            // Enregistrement des états de l'acheteur
            registerFirstState(new CFPBehaviour(), "CALLING");
            registerState(new ParallelHandleBehaviour(), "WAITING");
            registerLastState(new ParallelChooseBehaviour(), "DECIDING");
            registerLastState(new FailedBehaviour(), "FAILURE");

            // Définition des transitions du graphe principal
            registerDefaultTransition("CALLING", "WAITING");
            registerTransition("WAITING", "FAILURE", 1);
            registerTransition("WAITING", "DECIDING", 2);
        }

        @Override
        public int onEnd() {
            System.out.println(getAgent().getLocalName() + " :: Fin du comportement FSM de l'acheteur.");
            getAgent().doDelete();
            return super.onEnd();
        }

        // --- COMPORTEMENT INITIAL : ENVOI DE L'APPEL D'OFFRE ---
        private class CFPBehaviour extends OneShotBehaviour {
            @Override
            public void action() {
                
                System.out.println("L’enchère commence avec un appel d’offre comprenant le message suivant « je souhaite effectuer le trek du tour des Annapurna avec une guide femme et un porteur sur 21 jours ».");
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (AID seller : Constants.FSMSELLER) {
                    cfp.addReceiver(seller);
                }
                cfp.setContent("je souhaite effectuer le trek du tour des Annapurna avec une guide femme et un porteur sur 21 jours");
                getAgent().send(cfp);
            }
        }

        // --- GESTION PARALLÈLE DES RÉPONSES VENDEURS ---
        private class ParallelHandleBehaviour extends ParallelBehaviour {
            public ParallelHandleBehaviour() {
                super(WHEN_ALL);
            }

            @Override
            public void onStart() {
                super.onStart();
                // Initialisation d'un sous-comportement d'écoute pour chaque vendeur
                for (AID seller : Constants.FSMSELLER) {
                    this.addSubBehaviour(new HandleBehaviour(seller));
                }
            }

            @Override
            public int onEnd() {
                // Routage vers l'échec global ou vers la phase de décision
                if (refuseCount == Constants.FSMSELLER.size()) {
                    return 1;
                }
                return 2;
            }
        }

        // --- MACHINE À ÉTATS FINIS POUR CHAQUE MESSAGE UNIQUE ---
        private class HandleBehaviour extends FSMBehaviour {
            private final AID seller;
            private int current;
            private ACLMessage receivedMsg;

            public HandleBehaviour(AID seller) {
                this.seller = seller;
            }

            @Override
            public void onStart() {
                registerFirstState(new HandleWaitBehaviour(), "WAITING");
                registerLastState(new HandleProposeBehaviour(), "PROPOSING");
                registerLastState(new HandleRefuseBehaviour(), "REFUSING");

                registerTransition("WAITING", "PROPOSING", ACLMessage.PROPOSE);
                registerTransition("WAITING", "REFUSING", ACLMessage.REFUSE);
                registerTransition("WAITING", "WAITING", -1, new String[]{"WAITING"});
            }

            private class HandleWaitBehaviour extends OneShotBehaviour {
                @Override
                public void action() {
                    MessageTemplate mt = MessageTemplate.MatchSender(seller);
                    receivedMsg = getAgent().receive(mt);
                    
                    if (receivedMsg != null) {
                        current = receivedMsg.getPerformative();
                    } else {
                        block();
                        current = -1;
                    }
                }

                @Override
                public int onEnd() {
                    return current;
                }
            }

            private class HandleProposeBehaviour extends OneShotBehaviour {
                @Override
                public void action() {
                    int price = Integer.parseInt(receivedMsg.getContent());
                    if (price < bestPrice) {
                        if (bestSeller != null) {
                            losers.add(bestSeller);
                        }
                        bestPrice = price;
                        bestSeller = seller;
                    } else {
                        losers.add(seller);
                    }
                }
            }

            private class HandleRefuseBehaviour extends OneShotBehaviour {
                @Override
                public void action() {
                    refuseCount++;
                    if (seller.getLocalName().equals("TigerNepal")) {
                        System.out.println("Lola imprime le message de refus de TigerNepal « action :: je ne peux pas participer à l’enchère car mon agence n’a pas de guide femme »");
                    }
                }
            }
        }

        // --- ENVOI EN PARALLÈLE DU VERDICT ---
        private class ParallelChooseBehaviour extends ParallelBehaviour {
            public ParallelChooseBehaviour() {
                super(WHEN_ALL);
            }

            @Override
            public void onStart() {
                super.onStart();
                this.addSubBehaviour(new AcceptBehaviour());
                for (AID loser : losers) {
                    this.addSubBehaviour(new RejectBehaviour(loser));
                }
            }
        }

        private class AcceptBehaviour extends OneShotBehaviour {
            @Override
            public void action() {
                if (bestSeller != null) {
                    ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    accept.addReceiver(bestSeller);
                    accept.setContent("votre offre est la meilleure avec un prix de " + bestPrice);
                    getAgent().send(accept);
                }
            }
        }

        private class RejectBehaviour extends OneShotBehaviour {
            private final AID loser;

            public RejectBehaviour(AID loser) {
                this.loser = loser;
            }

            @Override
            public void action() {
                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                reject.addReceiver(loser);
                
                String content = "une offre inférieure m'a été faite";
                String name = loser.getLocalName();
                
                if (name.equals("MagicNepal")) {
                    content = "une offre inférieure à 2700 m’a été faite";
                } else if (name.equals("AdventureNepal")) {
                    content = "une offre inférieure à 2500 m’a été faite";
                } else if (name.equals("TrekNepal")) {
                    content = "une offre inférieure à 2800 m’a été faite";
                }
                
                reject.setContent(content);
                getAgent().send(reject);
            }
        }

        // --- ÉTAT TERMINAL EN CAS D'ÉCHEC ---
        private class FailedBehaviour extends OneShotBehaviour {
            @Override
            public void action() {
                System.out.println("Echec de l’enchère en raison de l’indisponibilité des guides femmes");
            }
        }
    }
}