package projet;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import static jade.core.behaviours.ParallelBehaviour.WHEN_ALL;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.List;

public class BuyerAgent extends DefaultAgent implements Constants {

    //gagnant courant mis de cote
    private ACLMessage winner = null;

    //perdants d'un tour de l'enchère suite au traitement du ParallelHandleMessage
    private List<ACLMessage> losers = new ArrayList<>();

    private ACLMessage message;
    private final List<AID> sellers;
    private int performative;

    /**
     * @return performative REQUEST_WHEN when a proposition is made
     */
    private int getPerformative() {
        return performative;
    }

    /**
     * @param performative the performative to set
     */
    private void setPerformative(int performative) {
        this.performative = performative;
    }

    /**
     * @param message current message
     */
    private void setMessage(ACLMessage message) {
        this.message = message;
    }

    /**
     * @return performative REQUEST_WHEN when a proposition is made
     */
    private ACLMessage getMessage() {
        return message;
    }

    /**
     * @return the currentWinner
     */
    private ACLMessage getWinner() {
        return winner;
    }

    /**
     * @param winner the currentWinner to set
     */
    private void setWinner(ACLMessage winner) {
        this.winner = winner;
    }

    /**
     * @return the roundLosers
     */
    private List<ACLMessage> getLosers() {
        return losers;
    }

    /**
     * @param losers the roundLosers to set
     */
    private void setLosers(List<ACLMessage> losers) {
        this.losers = losers;
    }

    /**
     * @return the sellers
     */
    private List<AID> getSellers() {
        return sellers;
    }

    @Override
    public String toString() {
        String value = "agent :: " + this.getLocalName();
        value += "\n Winner :: \n" + (getWinner() != null ? getWinner().getContent() : "Aucun");
        value += "\n Losers :: \n" + getLosers().size();
        return value;
    }

    //return the float price given a string value
    private float doPrice(String value) {
        return Float.parseFloat(value);
    }

    private void addAllReceivers(List<AID> receivers) {
        ACLMessage temp = getMessage();
        receivers.forEach(seller -> {
            temp.addReceiver(seller);
        });
    }

    /*
     * Constructeur BuyerAgent() initialise l'attriut sellers avec les vendeurs
     * initalise la performatrice de l'aget buyer avec ACLMessage.FAILURE
     */
    public BuyerAgent() {
        this.sellers = new ArrayList<>(FSMSELLER);
        this.performative = ACLMessage.FAILURE;
    }

    /*
     * méthode setup() appelle la méthode setup de la super-classe DefaultAgent
     * ajoute le comportement BuyerBehaviour
     * effectue l tracte suivante :trace("Initialisation de l'agent buyer  ",this);
     *
     */
    @Override
    public void setup() {
        super.setup();
        addBehaviour(new BuyerBehaviour(this));
        trace("Initialisation de l'agent buyer  ", this);
    }

    private class BuyerBehaviour extends FSMBehaviour {

        BuyerBehaviour(Agent a) {
            super(a);
        }

        /*
         méthode onStart() d'initialisation du comportement avec enregistrement
         des états et des transitions
         L’acheteur effectue un CFP dans l’état CALLING associé a comportement anonyme 
         qui envoie un CFP.
         La transition ACLMessage.CFP fait passer à l’état WAITING associé au comportement
         ParallelHandleBehaviour
         où les réponses des vendeurs sont traitées. En fin de ce comportement on a 
         le vendeur gagnant du tour de l’enchère et les perdants qui ont 
         envoyé un propose. 
         Les vendeurs ayant envoyés un refus ne sont pas considérés 
         comme perdants et ne recoivent plus de message.
         Si tous les vendeurs ont refusé la transition ACLMessage.FAILED fait passer 
         à l'état FAILED où un message d'échec de l'enchère est imprimé.
         La transition ACLMessage.REQUEST_WHEN fait passer à l’état DECIDING
         associé au comportement ParallelChooseBehaviour 
         ParallelChooseBehaviour est un comportement parallèle qui permet l'envoi
         d'un ACLMessage.ACCEPT_PROPOSAL au vendeur gagnant courant 
         et un ACLMessage.REJECT_PROPOSAL aux autres vendeurs restants
         */
        @Override
        public void onStart() {
            super.onStart();

            registerFirstState(new CfpBehaviour(getAgent()), "CALLING");
            registerState(new ParallelHandleBehaviour(getAgent()), "WAITING");
            registerLastState(new ParallelChooseBehaviour(getAgent()), "DECIDING");
            registerLastState(new FailedBehaviour(getAgent()), "FAILURE");

            registerTransition("CALLING", "WAITING", ACLMessage.CFP);
            registerTransition("WAITING", "DECIDING", ACLMessage.REQUEST_WHEN);
            registerTransition("WAITING", "FAILURE", ACLMessage.FAILURE);
        }

        /*
         * méthode onEnd()
         * méthodes de trace :
         * String agentInfo="Fin de vie Agent " + getAgent();"terminason du 
         * comportement avec le comportement et la représentation sus forme de 
         * caractères de l'agent
         * => voir méthodes trace de DefaultAgent
         * impression de fin de vie de l'agent
         * terminaison de l'agent
         */
        @Override
        public int onEnd() {
            String agentInfo = "Fin de vie Agent " + getAgent().getLocalName();
            trace("Terminaison du comportement  ", this, getAgent());
            trace(agentInfo);
            getAgent().doDelete();
            return super.onEnd();
        }

        class CfpBehaviour extends OneShotBehaviour {

            private static final String CONTENT = "je souhaite effectuer le trek du tour des Annapurna avec une guide femme et un porteur sur 21 jours";

            /**
             * Preparation du message CFP avec la liste des vendeurs en tant que
             * receveurs et "je recherche un guide femme pour le trek tour des
             * Annapurnas" comme contenu
             */
            CfpBehaviour(Agent a) {
                super(a);
            }

            @Override
            public void onStart() {
                ACLMessage temp = new ACLMessage(ACLMessage.CFP);
                doMessage(temp, CONTENT);
                trace("Initialisation du message d'appel d'offre ", this, this.getAgent());
            }

            //utilise la méthode addAllReceivers pour ajouter tous les vendeurs au message 
            private void doMessage(ACLMessage message, String content) {
                message.setProtocol("enchere-un-tour");
                message.setConversationId("guide-femme-Annapurna-Trek");
                message.setContent(content);
                setMessage(message);
                addAllReceivers(getSellers());
            }

            /*
             méthode action qui trace le message en envoi le message
             */
            @Override
            public void action() {
                getAgent().send(getMessage());
            }

            /*
             méthode de fin qui retourne la performative du message (CFP)
             */
            @Override
            public int onEnd() {
                trace("Terminaison du comportement ", this, getAgent());
                trace("Envoi performative CFP " + ACLMessage.getPerformative(ACLMessage.CFP));
                return ACLMessage.CFP;
            }
        }

        /**
         * Comportement FailedBehaviour à un coup qui impirme qu'aucun vendeur
         * ne paticpe à l'encère car pas de guide femme disponible
         */
        class FailedBehaviour extends OneShotBehaviour {

            FailedBehaviour(Agent a) {
                super(a);
            }

            @Override
            public void action() {
                trace("Echec de l'enchère en raison de l'indisponibilité des guides femmes");
            }
        }

        /**
         * Comportement ParallelChooseBehaviour parallèle qui comporte comme
         * premier élément un comportement à un coup anonyme qui créé une
         * réponse sur le message gagnant avec la performative ACCEPT_Proposal
         * et comme deuxième élément un comportement parallèle qui permet de
         * créer par message perdant une réponse avec la réponse REJECT_PROPOSAL
         * en prenant la proposition effecuée du messge perdant (prix) Ce second
         * comportement est terminé quand tous les comportements éléments sont
         * terminés Le comportement parallèle ParallelChooseBehaviour est
         * terminé quand ses deux comportements sont terminés.
         */
        class ParallelChooseBehaviour extends ParallelBehaviour {

            ParallelChooseBehaviour(Agent a) {
                super(a, WHEN_ALL);
            }

            @Override
            public void onStart() {
                super.onStart();

                if (getWinner() != null) {
                    this.addSubBehaviour(new OneShotBehaviour(getAgent()) {
                        @Override
                        public void action() {
                            ACLMessage accept = getWinner().createReply();
                            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            accept.setContent("votre offre est la meilleure avec un prix de " + getWinner().getContent());
                            getAgent().send(accept);
                        }
                    });
                }

                ParallelBehaviour rejectParallel = new ParallelBehaviour(getAgent(), WHEN_ALL);
                for (ACLMessage loserMsg : getLosers()) {
                    rejectParallel.addSubBehaviour(new OneShotBehaviour(getAgent()) {
                        @Override
                        public void action() {
                            ACLMessage reject = loserMsg.createReply();
                            reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            reject.setContent("une offre inférieure à " + loserMsg.getContent() + " m'a été faite");
                            getAgent().send(reject);
                        }
                    });
                }
                this.addSubBehaviour(rejectParallel);
            }
        }//end of ParallelChooseBehaviour 

        /**
         * Comportement ParallelHandleBehaviour Arrive dans cet état via la
         * transition CFP suite au comportement qui envoie le cfp A l'entrée de
         * ce comportement la liste des vendeurs getSellers comprend la liste
         * des vendeurs initiaux Pour chaque vendeurs un comportement
         * HandleSellerBehaviour (agent, vendeur) va être ajouté comme
         * comportement élément Le comportement ParallelHandleMessage se termine
         * lorsque tous les comportements HandleSellerBehaviour sont termines
         *
         */
        class ParallelHandleBehaviour extends ParallelBehaviour {

            ParallelHandleBehaviour(Agent a) {
                super(a, WHEN_ALL);
            }

            @Override
            public void onStart() {
                super.onStart();
                for (AID seller : getSellers()) {
                    this.addSubBehaviour(new HandleSellerBehaviour(getAgent(), seller));
                }
            }

            @Override
            public int onEnd() {
                if (getWinner() != null) {
                    setPerformative(ACLMessage.REQUEST_WHEN);
                } else {
                    setPerformative(ACLMessage.FAILURE);
                }
                return getPerformative();
            }
        }

        /**
         * Comportement HandleSellerBehaviour à états finis qui par vendeur
         * traite les réponses du vendeur qui peuvent être un propose ou un
         * refuse. Le comportement s'arrête dès qu'il a reçu une des deux
         * réponses qui sont les deux états terminaux * Voir
         * HandleMessageBehaviour
         */
        class HandleSellerBehaviour extends FSMBehaviour { // Handle propose et refuse messages

            private AID sender;
            private ACLMessage receivedMessage;

            HandleSellerBehaviour(Agent a, AID seller) {
                super(a);
                setSender(seller);
            }

            private void setReceivedMessage(ACLMessage message) {
                this.receivedMessage = message;
            }

            private ACLMessage getReceivedMessage() {
                return this.receivedMessage;
            }

            private void setSender(AID sender) {
                this.sender = sender;
            }

            private AID getSender() {
                return this.sender;
            }

            @Override
            public void onStart() {
                super.onStart();
                registerFirstState(new HandleMessageBehaviour(getAgent()), "WAITING");
                registerLastState(new HandleProposeBehaviour(getAgent()), "PROPOSING");
                registerLastState(new HandleRefuseBehaviour(getAgent()), "REFUSING");

                registerTransition("WAITING", "PROPOSING", ACLMessage.PROPOSE);
                registerTransition("WAITING", "REFUSING", ACLMessage.REFUSE);
            }

            @Override
            public int onEnd() {
                return 1;
            }

            /**
             * Comportement HandleMessageBehaviour comportement simple qui
             * reçoit le message et retourne la performative reçu et bloque le
             * comportement si message nul
             *
             */
            class HandleMessageBehaviour extends SimpleBehaviour {

                private MessageTemplate template;
                private boolean finished = false;
                private int exitValue = 0;

                HandleMessageBehaviour(Agent a) {
                    super(a);
                    setTemplate(MessageTemplate.MatchSender(getSender()));
                }

                @Override
                public void action() {
                    ACLMessage msg = getAgent().receive(getTemplate());
                    if (msg != null) {
                        setReceivedMessage(msg);
                        exitValue = msg.getPerformative();
                        finished = true;
                    } else {
                        block();
                    }
                }

                @Override
                public boolean done() {
                    return finished;
                }

                @Override
                public int onEnd() {
                    return exitValue;
                }

                /**
                 * @return the template
                 */
                private MessageTemplate getTemplate() {
                    return template;
                }

                /**
                 * @param template the template to set
                 */
                private void setTemplate(MessageTemplate template) {
                    this.template = template;
                }
            }

            /**
             * Comportement HandleProposeBehaviour à un coup qui traite la
             * proposition du vendeur : si c'est la première proposition elle
             * est la proposition gagnante courante (setWinner) et la
             * performative de l'agent est établie à REQUEST_WHEN sinon si le
             * prix est le plus fible (isWinner) alors elle est dite meilleure
             * et la précédente gagnante est ajouter aux perdants (doWinner)
             * sinon elle est perdante (doLoser)
             */
            class HandleProposeBehaviour extends OneShotBehaviour {

                HandleProposeBehaviour(Agent a) {
                    super(a);
                }

                @Override
                public void action() {
                    ACLMessage msg = getReceivedMessage();
                    if (isFirst()) {
                        setWinner(msg);
                    } else if (isWinner(msg)) {
                        doWinner(msg);
                    } else {
                        doLoser(msg);
                    }
                    trace("Traite le message recu en gagnant ou perdant", this, getAgent());
                }//end action

                /**
                 * terminaison du comportement utiliser les méthodes de trace
                 * permettant d'imprimer le gagnant courant et les perdants
                 *
                 * @return
                 */
                @Override
                public int onEnd() {
                    trace("Terminaison du comportement : \n Gagnant courant : " + (getWinner() != null ? getWinner().getContent() : "Aucun") + "\n Nombre de perdants : " + getLosers().size());
                    return super.onEnd();
                }

                private boolean isFirst() {
                    return getWinner() == null;
                }

                private boolean isWinner(ACLMessage message) {
                    return isWinner(message.getContent());
                }

                private boolean isWinner(String proposition) {
                    return doPrice(getWinner().getContent()) > doPrice(proposition);
                }

                private void doWinner(ACLMessage message) {
                    doLoser(getWinner());
                    setWinner(message);
                }

                private void doLoser(ACLMessage message) {
                    getLosers().add(message);
                }
            }// end of HandleProposeBehaviour

            class HandleRefuseBehaviour extends OneShotBehaviour {

                HandleRefuseBehaviour(Agent a) {
                    super(a);
                }

                @Override
                public void action() {
                    trace("action :: je ne peux pas participer à l'enchère car mon agence n'a pas de guide femme (" + getSender().getLocalName() + ")");
                }
            }//HandleRefuseBehaviour 
        }//end of HandleSellerBehaviour class
    }//end class ParallelHandleBehaviour
}//BuyerAgent
