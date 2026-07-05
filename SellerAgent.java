package testjade;

import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class SellerAgent extends DefaultAgent {

    @Override 
    protected void setup() {
        super.setup();
        addBehaviour(new SellerBehaviour(this));
    }

    private static class SellerBehaviour extends FSMBehaviour {

        // Variables partagées entre les états
        private ACLMessage currentMessage;
        private int proposedPrice;

        public SellerBehaviour(SellerAgent a) {
            super(a);
        }
        
        @Override
        public void onStart() {
            super.onStart();

            // Enregistrement des états
            registerFirstState(new HandleMessageBehaviour(), "WAITING");
            registerState(new ProposingBehaviour(), "PROPOSING");
            registerLastState(new RefuseBehaviour(), "REFUSING");
            registerLastState(new WinnerBehaviour(), "WINNING");
            registerLastState(new LoserBehaviour(), "LOSING");

            // Transitions
            registerTransition("WAITING", "PROPOSING", ACLMessage.PROPOSE);
            registerTransition("WAITING", "REFUSING", ACLMessage.REFUSE);
            registerTransition("WAITING", "WINNING", ACLMessage.ACCEPT_PROPOSAL);
            registerTransition("WAITING", "LOSING", ACLMessage.REJECT_PROPOSAL);
            
            // Transitions par défaut pour boucler ou revenir en attente
            registerTransition("WAITING", "WAITING", -1, new String[]{"WAITING"});
            registerDefaultTransition("PROPOSING", "WAITING", new String[]{"WAITING"});
        }
        
        @Override
        public int onEnd() {
            System.out.println(getAgent().getLocalName() + " :: Fin du comportement FSM du vendeur.");
            getAgent().doDelete();
            return super.onEnd();
        }

        // --- ÉTAT CENTRAL : RÉCEPTION ET ROUTAGE ---
        
        private class HandleMessageBehaviour extends OneShotBehaviour {
            private int current;

            @Override
            public void action() {
                currentMessage = getAgent().receive();
                
                if (currentMessage != null) {
                    int perf = currentMessage.getPerformative();
                    
                    if (perf == ACLMessage.CFP) {
                        String name = getAgent().getLocalName();
                        
                        if (name.equals("TigerNepal")) {
                            current = ACLMessage.REFUSE;
                        } else {
                            proposedPrice = switch (name) {
                                case "MagicNepal" -> 2700;
                                case "AdventureNepal" -> 2800;
                                case "TrekNepal" -> 2500;
                                case "TreeSisters" -> 2400;
                                default -> 3000;
                            };
                            
                            current = ACLMessage.PROPOSE;
                        }
                    } else {
                        // On transmet la performative reçue (ex: ACCEPT_PROPOSAL ou REJECT_PROPOSAL)
                        current = perf;
                    }
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

        // --- ÉTATS DE TRAITEMENT ---

        private class ProposingBehaviour extends OneShotBehaviour {
            @Override
            public void action() {
                ACLMessage reply = currentMessage.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(String.valueOf(proposedPrice));
                getAgent().send(reply);
            }
        }

        private class RefuseBehaviour extends OneShotBehaviour {
            @Override
            public void action() {
                ACLMessage reply = currentMessage.createReply();
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("action :: je ne peux pas participer à l’enchère car mon agence n’a pas de guide femme");
                getAgent().send(reply);
                
                System.out.println(getAgent().getLocalName() + " imprime le message de refus : action :: je ne peux pas participer à l’enchère car mon agence n’a pas de guide femme");
            }
        }

        private class WinnerBehaviour extends OneShotBehaviour {
            @Override
            public void action() {
                // Lecture du message envoyé par l'acheteur
                String buyerMessage = currentMessage.getContent();
                System.out.println(getAgent().getLocalName() + " a reçu le message de l'acheteur : " + buyerMessage);
                
                // Impression du message de victoire requis par le sujet
                System.out.println("action :: " + getAgent().getLocalName() + " est gagnant avec une proposition de " + proposedPrice + " euros" );
            }
        }

        private class LoserBehaviour extends OneShotBehaviour {
            @Override
            public void action() {
                // Lecture du message de rejet envoyé par l'acheteur
                String buyerMessage = currentMessage.getContent();
                System.out.println(getAgent().getLocalName() + " a reçu la raison du rejet : " + buyerMessage);
                
                // Impression du message de défaite requis par le sujet
                System.out.println("action :: " + getAgent().getLocalName() + " est perdant avec une proposition de " + proposedPrice);
            }
        }
    }
}